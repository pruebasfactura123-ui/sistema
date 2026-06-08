package com.example.sistema.controller;

import com.example.sistema.model.Nomina;
import com.example.sistema.model.Usuario;
import com.example.sistema.model.Auditoria;
import com.example.sistema.repository.NominaRepository;
import com.example.sistema.repository.UsuarioRepository;
import com.example.sistema.repository.AuditoriaRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/operaciones")
public class NominaController {

    @Autowired
    private NominaRepository nominaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    /**
     * Muestra el historial de nóminas y el formulario para simular/generar una nueva
     */
    @GetMapping("/nominas")
    public String listarNominas(Model model) {
        List<Nomina> nominas = nominaRepository.findAllByOrderByFechaEmisionDesc();
        List<Usuario> trabajadores = usuarioRepository.findAll(); // Cargar los empleados existentes

        model.addAttribute("nominas", nominas);
        model.addAttribute("trabajadores", trabajadores);
        model.addAttribute("empresaNombre", "OFICINA FISCAL (Nóminas)");
        return "nominas"; // Buscará nominas.html en templates
    }

    /**
     * Procesa el registro o simulación de un recibo de nómina incluyendo incidencias
     */
    @PostMapping("/nominas/guardar")
    public String guardarNomina(@RequestParam("trabajadorId") Long trabajadorId,
                                @RequestParam("periodo") String periodo,
                                @RequestParam("sueldoBase") Double sueldoBase,
                                @RequestParam("diasTrabajados") Integer diasTrabajados,
                                @RequestParam("horasExtra") Integer horasExtra,
                                @RequestParam("faltas") Integer faltas,
                                @RequestParam("retardos") Integer retardos,
                                @RequestParam("percepciones") Double percepciones,
                                @RequestParam("deducciones") Double deducciones,
                                @RequestParam("estado") String estado,
                                @RequestParam("fechaEmision") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaEmision,
                                Authentication authentication) {
        
        Usuario trabajador = usuarioRepository.findById(trabajadorId).orElse(null);
        
        if (trabajador != null) {
            Nomina nueva = new Nomina();
            nueva.setTrabajador(trabajador);
            nueva.setPeriodo(periodo);
            nueva.setSueldoBase(sueldoBase);
            
            // Guardar incidencias en la entidad
            nueva.setDiasTrabajados(diasTrabajados);
            nueva.setHorasExtra(horasExtra);
            nueva.setFaltas(faltas);
            nueva.setRetardos(retardos);

            // CÁLCULO MATEMÁTICO BASADO EN INCIDENCIAS
            // 1. Sumamos $100 MXN por cada hora extra a las percepciones capturadas en el formulario
            Double totalPercepciones = percepciones + (horasExtra * 100.0);
            
            // 2. Descontamos un día completo de salario por cada falta cometida
            // Se asume un periodo quincenal (15 días) para calcular el salario diario base
            Double sueldoDiario = sueldoBase / 15.0;
            Double totalDeducciones = deducciones + (faltas * sueldoDiario);

            nueva.setPercepciones(totalPercepciones);
            nueva.setDeducciones(totalDeducciones);

            // CÁLCULO AUTOMÁTICO DEL SUELDO NETO TOTAL
            Double neto = sueldoBase + totalPercepciones - totalDeducciones;
            nueva.setSueldoNeto(neto);
            
            nueva.setEstado(estado);
            nueva.setFechaEmision(fechaEmision);

            nominaRepository.save(nueva);

            // ==================== GUARDAR REGISTRO EN AUDITORÍA ====================
            String usuarioActivo = (authentication != null) ? authentication.getName() : "Sistema";
            String detalles = "Generó una nómina para el empleado '" + trabajador.getUsername() 
                            + "' correspondiente al periodo '" + periodo 
                            + "' con un sueldo neto calculado de $" + String.format("%.2f", neto);
            
            Auditoria registro = new Auditoria(usuarioActivo, "CREAR NÓMINA", detalles);
            auditoriaRepository.save(registro);
            // =======================================================================
        }

        return "redirect:/operaciones/nominas";
    }

    /**
     * Genera un recibo de nómina en formato PDF utilizando OpenPDF incluyendo el desglose de incidencias
     */
    @GetMapping("/nominas/descargar/{id}")
    public ResponseEntity<byte[]> descargarReciboPdf(@PathVariable("id") Long id) {
        Nomina nomina = nominaRepository.findById(id).orElse(null);
        if (nomina == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fuentes para el diseño
            Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtituloFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font cuerpoFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // Encabezado del Recibo
            Paragraph titulo = new Paragraph("OFICINA FISCAL", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("RECIBO DE NÓMINA SIMULADO", subtituloFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            document.add(subtitulo);

            // Tabla de Datos Generales e Incidencias
            PdfPTable tableGeneral = new PdfPTable(2);
            tableGeneral.setWidthPercentage(100);
            tableGeneral.setSpacingAfter(15);
            
            tableGeneral.addCell(new Paragraph("Empleado:", cuerpoFont));
            tableGeneral.addCell(new Paragraph(nomina.getTrabajador().getUsername(), cuerpoFont));
            
            tableGeneral.addCell(new Paragraph("Periodo:", cuerpoFont));
            tableGeneral.addCell(new Paragraph(nomina.getPeriodo(), cuerpoFont));

            tableGeneral.addCell(new Paragraph("Días Trabajados:", cuerpoFont));
            tableGeneral.addCell(new Paragraph(String.valueOf(nomina.getDiasTrabajados()), cuerpoFont));

            tableGeneral.addCell(new Paragraph("Horas Extra / Faltas / Retardos:", cuerpoFont));
            tableGeneral.addCell(new Paragraph(nomina.getHorasExtra() + " hrs / " + nomina.getFaltas() + " faltas / " + nomina.getRetardos() + " ret.", cuerpoFont));
            
            tableGeneral.addCell(new Paragraph("Fecha de Emisión:", cuerpoFont));
            tableGeneral.addCell(new Paragraph(nomina.getFechaEmision().toString(), cuerpoFont));
            
            tableGeneral.addCell(new Paragraph("Estado de Pago:", cuerpoFont));
            tableGeneral.addCell(new Paragraph(nomina.getEstado(), cuerpoFont));
            
            document.add(tableGeneral);

            // Tabla de desglose de conceptos financieros
            PdfPTable tableDesglose = new PdfPTable(2);
            tableDesglose.setWidthPercentage(100);
            tableDesglose.setSpacingAfter(20);

            tableDesglose.addCell(new Paragraph("Sueldo Base:", cuerpoFont));
            tableDesglose.addCell(new Paragraph("$" + String.format("%.2f", nomina.getSueldoBase()), cuerpoFont));

            tableDesglose.addCell(new Paragraph("(+) Percepciones Totales (Inc. Hrs Extra):", cuerpoFont));
            tableDesglose.addCell(new Paragraph("$" + String.format("%.2f", nomina.getPercepciones()), cuerpoFont));

            tableDesglose.addCell(new Paragraph("(-) Deducciones Totales (Inc. Faltas):", cuerpoFont));
            tableDesglose.addCell(new Paragraph("$" + String.format("%.2f", nomina.getDeducciones()), cuerpoFont));

            Font netoFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            tableDesglose.addCell(new Paragraph("SUELDO NETO A RECIBIR:", netoFont));
            tableDesglose.addCell(new Paragraph("$" + String.format("%.2f", nomina.getSueldoNeto()), netoFont));

            document.add(tableDesglose);

            // Nota al pie
            Paragraph pie = new Paragraph("Este documento es una simulación interna de la Oficina Fiscal y carece de validez fiscal ante el SAT.", new Font(Font.HELVETICA, 8, Font.ITALIC));
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();

            // Configurar los encabezados de respuesta HTTP para forzar la descarga del PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "Recibo_Nomina_" + nomina.getTrabajador().getUsername().replace(" ", "_") + "_" + nomina.getPeriodo() + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Elimina un registro de nómina
     */
    @GetMapping("/nominas/eliminar/{id}")
    public String eliminarNomina(@PathVariable("id") Long id, Authentication authentication) {
        Nomina nomina = nominaRepository.findById(id).orElse(null);

        if (nomina != null) {
            // ==================== GUARDAR REGISTRO EN AUDITORÍA ====================
            String usuarioActivo = (authentication != null) ? authentication.getName() : "Sistema";
            String detalles = "Eliminó el registro de nómina del empleado '" + nomina.getTrabajador().getUsername() 
                            + "' correspondiente al periodo '" + nomina.getPeriodo() 
                            + "' por un monto de $" + String.format("%.2f", nomina.getSueldoNeto());
            
            Auditoria registro = new Auditoria(usuarioActivo, "ELIMINAR NÓMINA", detalles);
            auditoriaRepository.save(registro);
            // =======================================================================

            nominaRepository.deleteById(id);
        }

        return "redirect:/operaciones/nominas";
    }
}