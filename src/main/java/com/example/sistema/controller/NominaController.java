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
     * RUTA UNIFICADA:
     * - Si el usuario es JEFE, GERENTE o ADMIN -> Muestra el panel completo ("nominas").
     * - Si el usuario es TRABAJADOR / EMPLEADO -> Muestra su vista simplificada ("mis-nominas").
     */
    @GetMapping("/nominas")
    public String listarNominas(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String usernameActivo = authentication.getName();
        Usuario usuarioLogueado = usuarioRepository.findByUsername(usernameActivo).orElse(null);

        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuarioLogueado", usuarioLogueado);

        // Verificamos el rol del usuario (soporta prefijos ROLE_ o roles limpios en DB)
        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_JEFE") || 
                               a.getAuthority().equals("ROLE_GERENTE") || 
                               a.getAuthority().equals("ROLE_ADMIN")) ||
                "JEFE".equals(usuarioLogueado.getRol()) || 
                "GERENTE".equals(usuarioLogueado.getRol()) || 
                "ADMIN".equals(usuarioLogueado.getRol());

        if (esJefe) {
            // 1. VISTA ADMINISTRATIVA
            Long empresaId = (usuarioLogueado.getEmpresa() != null) ? usuarioLogueado.getEmpresa().getId() : null;
            List<Nomina> nominas = nominaRepository.findByTrabajadorEmpresaIdOrderByFechaEmisionDesc(empresaId);
            List<Usuario> trabajadores = usuarioRepository.findByEmpresaId(empresaId);

            model.addAttribute("nominas", nominas);
            model.addAttribute("trabajadores", trabajadores);

            String nombreEmpresa = "OFICINA FISCAL";
            if (usuarioLogueado.getEmpresa() != null && usuarioLogueado.getEmpresa().getRazonSocial() != null) {
                nombreEmpresa = usuarioLogueado.getEmpresa().getRazonSocial();
            }
            model.addAttribute("empresaNombre", nombreEmpresa);

            return "nominas"; // Carga nominas.html (Vista de gestión)
        } else {
            // 2. VISTA DEL TRABAJADOR REGULAR
            List<Nomina> misNominas = nominaRepository.findByTrabajadorOrderByFechaEmisionDesc(usuarioLogueado);
            model.addAttribute("recibos", misNominas);

            return "mis-nominas"; // Carga mis-nominas.html (Vista de consulta de sus recibos)
        }
    }

    /**
     * Procesar el registro seguro de un recibo de nómina validando la pertenencia de empresa
     * e impidiendo la generación de nóminas duplicadas para un mismo trabajador y periodo.
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

        if (authentication == null) return "redirect:/login";

        String usuarioActivo = authentication.getName();
        Usuario usuarioLogueado = usuarioRepository.findByUsername(usuarioActivo).orElse(null);
        Usuario trabajador = usuarioRepository.findById(trabajadorId).orElse(null);

        if (usuarioLogueado != null && trabajador != null) {

            // BLINDAJE 1: Evitar que mediante alteraciones externas se registre un empleado de otra empresa
            if (usuarioLogueado.getEmpresa() != null && trabajador.getEmpresa() != null &&
                !trabajador.getEmpresa().getId().equals(usuarioLogueado.getEmpresa().getId())) {
                return "redirect:/operaciones/nominas?error=NoAutorizado";
            }

            // BLINDAJE 2: Validación en Backend contra Nóminas Duplicadas (Mismo trabajador y periodo)
            boolean existeDuplicado = nominaRepository.existsByTrabajadorIdAndPeriodo(trabajadorId, periodo);

            if (existeDuplicado) {
                return "redirect:/operaciones/nominas?errorDuplicado=true";
            }

            Nomina nueva = new Nomina();
            nueva.setTrabajador(trabajador);
            nueva.setPeriodo(periodo);
            nueva.setSueldoBase(sueldoBase);
            nueva.setDiasTrabajados(diasTrabajados);
            nueva.setHorasExtra(horasExtra);
            nueva.setFaltas(faltas);
            nueva.setRetardos(retardos);

            // CÁLCULO MATEMÁTICO BASADO EN INCIDENCIAS
            Double totalPercepciones = percepciones + (horasExtra * 100.0);
            Double sueldoDiario = sueldoBase / 15.0;
            Double totalDeducciones = deducciones + (faltas * sueldoDiario);

            nueva.setPercepciones(totalPercepciones);
            nueva.setDeducciones(totalDeducciones);

            Double neto = sueldoBase + totalPercepciones - totalDeducciones;
            nueva.setSueldoNeto(neto);

            nueva.setEstado(estado);
            nueva.setFechaEmision(fechaEmision);

            nominaRepository.save(nueva);

            // REGISTRO EN AUDITORÍA TRACEABLE POR EMPRESA
            String detalles = "Generó una nómina para el empleado '" + trabajador.getUsername()
                            + "' correspondiente al periodo '" + periodo
                            + "' con un sueldo neto calculado de $" + String.format("%.2f", neto);

            Auditoria registro = new Auditoria(usuarioActivo, "CREAR NÓMINA", detalles, usuarioLogueado.getEmpresa());
            auditoriaRepository.save(registro);

            return "redirect:/operaciones/nominas?exito=true";
        }

        return "redirect:/operaciones/nominas";
    }

    /**
     * Genera un recibo de nómina en formato PDF validando que pertenezca a la empresa del usuario
     */
    @GetMapping("/nominas/descargar/{id}")
    public ResponseEntity<byte[]> descargarReciboPdf(@PathVariable("id") Long id, Authentication authentication) {
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String usernameActivo = authentication.getName();
        Usuario usuarioLogueado = usuarioRepository.findByUsername(usernameActivo).orElse(null);
        Nomina nomina = nominaRepository.findById(id).orElse(null);

        if (nomina == null || nomina.getTrabajador() == null || usuarioLogueado == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // BLINDAJE: Si no es Admin global y la nómina no es de su propia empresa, bloquear acceso
        if (!"ADMIN".equals(usuarioLogueado.getRol())) {
            Long empresaUsuario = (usuarioLogueado.getEmpresa() != null) ? usuarioLogueado.getEmpresa().getId() : null;
            Long empresaNomina = (nomina.getTrabajador().getEmpresa() != null) ? nomina.getTrabajador().getEmpresa().getId() : null;

            // Validar si es un empleado común tratando de husmear la nómina de otro
            if ("EMPLEADO".equals(usuarioLogueado.getRol()) && !nomina.getTrabajador().getId().equals(usuarioLogueado.getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            // Validar si es un Jefe intentando descargar datos de otra empresa distinta
            if (empresaUsuario != null && !empresaUsuario.equals(empresaNomina)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtituloFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font cuerpoFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            String nombreEmpresaPdf = "OFICINA FISCAL";
            if (nomina.getTrabajador().getEmpresa() != null && nomina.getTrabajador().getEmpresa().getRazonSocial() != null) {
                nombreEmpresaPdf = nomina.getTrabajador().getEmpresa().getRazonSocial().toUpperCase();
            }
            Paragraph titulo = new Paragraph(nombreEmpresaPdf, tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("RECIBO DE NÓMINA SIMULADO", subtituloFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            document.add(subtitulo);

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

            Paragraph pie = new Paragraph("Este documento es una simulación interna y carece de validez fiscal oficial ante el SAT.", new Font(Font.HELVETICA, 8, Font.ITALIC));
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String empleadoNombre = nomina.getTrabajador().getUsername().replace(" ", "_");
            String filename = "Recibo_Nomina_" + empleadoNombre + "_" + nomina.getPeriodo() + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Elimina un registro de nómina asegurando protección perimetral multiempresa
     */
    @GetMapping("/nominas/eliminar/{id}")
    public String eliminarNomina(@PathVariable("id") Long id, Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        String usuarioActivo = authentication.getName();
        Usuario usuarioLogueado = usuarioRepository.findByUsername(usuarioActivo).orElse(null);
        Nomina nomina = nominaRepository.findById(id).orElse(null);

        if (nomina != null && usuarioLogueado != null) {

            // BLINDAJE: Bloquear si un jefe intenta mandar un ID por URL de una nómina ajena
            if (!"ADMIN".equals(usuarioLogueado.getRol())) {
                Long empresaUsuario = (usuarioLogueado.getEmpresa() != null) ? usuarioLogueado.getEmpresa().getId() : null;
                Long empresaNomina = (nomina.getTrabajador().getEmpresa() != null) ? nomina.getTrabajador().getEmpresa().getId() : null;

                if (empresaUsuario != null && !empresaUsuario.equals(empresaNomina)) {
                    return "redirect:/operaciones/nominas?error=NoAutorizado";
                }
            }

            String empleadoNombre = (nomina.getTrabajador() != null) ? nomina.getTrabajador().getUsername() : "Empleado no asignado";

            String detalles = "Eliminó el registro de nómina del empleado '" + empleadoNombre 
                            + "' correspondiente al periodo '" + nomina.getPeriodo() 
                            + "' por un monto de $" + String.format("%.2f", nomina.getSueldoNeto());

            Auditoria registro = new Auditoria(usuarioActivo, "ELIMINAR NÓMINA", detalles, usuarioLogueado.getEmpresa());
            auditoriaRepository.save(registro);

            nominaRepository.deleteById(id);
        }

        return "redirect:/operaciones/nominas";
    }
}