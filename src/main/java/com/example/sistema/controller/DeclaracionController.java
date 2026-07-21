package com.example.sistema.controller;

import com.example.sistema.model.Declaracion;
import com.example.sistema.model.Empresa;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.DeclaracionRepository;
import com.example.sistema.repository.UsuarioRepository;
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
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/operaciones")
public class DeclaracionController {

    @Autowired
    private DeclaracionRepository declaracionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Directorio local en el disco C:/ para almacenar los PDFs físicos de los acuses del SAT
    private final String UPLOAD_DIR = "C:/sistema_contable/acuses_sat/";

    /**
     * Helper para obtener el usuario logueado en el sistema con el parche para el admin de pruebas
     */
    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) throw new RuntimeException("No hay ninguna sesión activa.");
        
        // Salvavidas para el admin de pruebas en memoria
        if ("admin".equalsIgnoreCase(principal.getName())) {
            Usuario adminFicticio = new Usuario();
            adminFicticio.setUsername("admin");
            adminFicticio.setRol("JEFE");
            
            Empresa emp = new Empresa();
            emp.setId(1L); // ID de la empresa de pruebas
            emp.setRazonSocial("Empresa de Prueba (Admin)");
            adminFicticio.setEmpresa(emp);
            return adminFicticio;
        }

        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    /**
     * Muestra la pantalla principal con la lista de declaraciones de la empresa activa
     */
    @GetMapping("/declaraciones")
    public String listarDeclaraciones(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarioLogueado", logueado);

            if (logueado.getEmpresa() == null) {
                model.addAttribute("declaraciones", new ArrayList<>());
                model.addAttribute("empresaNombre", "Sin Empresa Asignada");
            } else {
                Long empresaId = logueado.getEmpresa().getId();
                List<Declaracion> declaraciones = declaracionRepository.findByEmpresaIdOrderByAnioDescMesDesc(empresaId);
                
                model.addAttribute("declaraciones", declaraciones);
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("declaraciones", new ArrayList<>());
            model.addAttribute("empresaNombre", "Error al cargar empresa");
        }

        return "declaraciones"; 
    }

    /**
     * Procesa el formulario para registrar una nueva obligación/declaración fiscal.
     * Incluye validación de duplicados.
     */
    @PostMapping("/declaraciones/guardar")
    public String guardarDeclaracion(Principal principal,
                                     @RequestParam("anio") Integer anio,
                                     @RequestParam("mes") String mes,
                                     @RequestParam("tipo") String tipo,
                                     @RequestParam("fechaVencimiento") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaVencimiento) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            Usuario logueado = getUsuarioLogueado(principal);
            if (logueado.getEmpresa() != null) {
                Long empresaId = logueado.getEmpresa().getId();

                // 1. VALIDACIÓN CONTRA DUPLICADOS
                boolean existeDuplicado = declaracionRepository.existsByEmpresaIdAndAnioAndMesAndTipo(
                        empresaId, anio, mes, tipo
                );

                if (existeDuplicado) {
                    return "redirect:/operaciones/declaraciones?errorDuplicado=true";
                }

                // 2. GUARDADO DE LA NUEVA DECLARACIÓN
                Declaracion nueva = new Declaracion();
                nueva.setAnio(anio);
                nueva.setMes(mes);
                nueva.setTipo(tipo);
                nueva.setFechaVencimiento(fechaVencimiento);
                nueva.setEstado("PENDIENTE");
                nueva.setEmpresa(logueado.getEmpresa());

                declaracionRepository.save(nueva);
                return "redirect:/operaciones/declaraciones?exito=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/operaciones/declaraciones";
    }

    /**
     * Sube el acuse PDF del SAT y cambia el estado de la declaración a 'PRESENTADA'
     */
    @PostMapping("/declaraciones/presentar/{id}")
    public String presentarDeclaracion(@PathVariable("id") Long id,
                                       @RequestParam("archivoAcuse") MultipartFile archivo) {
        
        Optional<Declaracion> decOpt = declaracionRepository.findById(id);
        
        if (decOpt.isPresent() && !archivo.isEmpty()) {
            Declaracion dec = decOpt.get();
            try {
                // Validar extensión básica (.pdf)
                String originalFilename = archivo.getOriginalFilename();
                if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
                    return "redirect:/operaciones/declaraciones?errorExtension=true";
                }

                // Crear carpeta física en C:/ si no existe
                File directorio = new File(UPLOAD_DIR);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                // Definir nombre único para el PDF del acuse
                String nombreArchivo = "acuse_dec_" + id + "_" + System.currentTimeMillis() + ".pdf";
                Path rutaCompleta = Paths.get(UPLOAD_DIR + nombreArchivo);
                
                // Guardar el archivo en el disco local
                Files.write(rutaCompleta, archivo.getBytes());

                // Actualizar datos de la BD
                dec.setRutaAcusePdf(nombreArchivo);
                dec.setEstado("PRESENTADA");
                dec.setFechaPresentacion(LocalDate.now());

                declaracionRepository.save(dec);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return "redirect:/operaciones/declaraciones";
    }

    /**
     * Transmite el archivo PDF almacenado en C:/ directamente al navegador
     */
    @GetMapping("/acuses/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> descargarAcuse(@PathVariable String filename) {
        try {
            Path file = Paths.get(UPLOAD_DIR).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
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