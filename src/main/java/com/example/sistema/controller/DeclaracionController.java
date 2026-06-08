package com.example.sistema.controller;

import com.example.sistema.model.Declaracion;
import com.example.sistema.model.Empresa;
import com.example.sistema.repository.DeclaracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/operaciones")
public class DeclaracionController {

    @Autowired
    private DeclaracionRepository declaracionRepository;

    // Directorio local en el disco C:/ para almacenar los PDFs físicos de los acuses del SAT
    private final String UPLOAD_DIR = "C:/sistema_contable/acuses_sat/";

    /**
     * Muestra la pantalla principal con la lista de declaraciones de la empresa activa
     */
    @GetMapping("/declaraciones")
    public String listarDeclaraciones(Model model) {
        // TODO: Simulación del ID de la empresa activa (Sustituir por tu lógica de sesión o usuario logueado)
        Long empresaId = 1L; 
        
        List<Declaracion> declaraciones = declaracionRepository.findByEmpresaIdOrderByAnioDescMesDesc(empresaId);
        
        model.addAttribute("declaraciones", declaraciones);
        model.addAttribute("empresaNombre", "OFICINA FISCAL (Declaraciones)");
        return "declaraciones"; // Esto buscará el archivo declaraciones.html en templates
    }

    /**
     * Procesa el formulario para registrar una nueva obligación/declaración fiscal
     */
    @PostMapping("/declaraciones/guardar")
    public String guardarDeclaracion(@RequestParam("anio") Integer anio,
                                     @RequestParam("mes") String mes,
                                     @RequestParam("tipo") String tipo,
                                     @RequestParam("fechaVencimiento") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaVencimiento) {
        
        Declaracion nueva = new Declaracion();
        nueva.setAnio(anio);
        nueva.setMes(mes);
        nueva.setTipo(tipo);
        nueva.setFechaVencimiento(fechaVencimiento);
        nueva.setEstado("PENDIENTE"); // Toda declaración inicia como pendiente

        // Asignar empresa (Simulado, cambiar por tu lógica real)
        Empresa e = new Empresa();
        e.setId(1L);
        nueva.setEmpresa(e);

        declaracionRepository.save(nueva);
        return "redirect:/operaciones/declaraciones";
    }

    /**
     * Sube el acuse PDF del SAT y cambia el estado de la declaración a 'PRESENTADA'
     */
    @PostMapping("/declaraciones/presentar/{id}")
    public String presentarDeclaracion(@PathVariable("id") Long id,
                                       @RequestParam("archivoAcuse") MultipartFile archivo) {
        
        Declaracion dec = declaracionRepository.findById(id).orElse(null);
        
        if (dec != null && !archivo.isEmpty()) {
            try {
                // Crear carpeta física si no existe en el disco duro
                File directorio = new File(UPLOAD_DIR);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                // Definir nombre único para el PDF del acuse
                String nombreArchivo = "acuse_dec_" + id + "_" + System.currentTimeMillis() + ".pdf";
                Path rutaCompleta = Paths.get(UPLOAD_DIR + nombreArchivo);
                
                // Guardar el archivo en el disco local
                Files.write(rutaCompleta, archivo.getBytes());

                // Actualizar los datos del registro en la BD
                dec.setRutaAcusePdf(nombreArchivo);
                dec.setEstado("PRESENTADA");
                dec.setFechaPresentacion(LocalDate.now()); // Se presenta hoy

                declaracionRepository.save(dec);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return "redirect:/operaciones/declaraciones";
    }

    /**
     * Método nuevo: Lee el PDF desde el disco C:/ y lo transmite al navegador 
     * para que no de pantalla blanca (Whitelabel Error).
     */
    @GetMapping("/acuses/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> descargarAcuse(@PathVariable String filename) {
        try {
            Path file = Paths.get(UPLOAD_DIR).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        // "inline" hace que el navegador lo intente abrir en pantalla en vez de forzar descarga
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
}