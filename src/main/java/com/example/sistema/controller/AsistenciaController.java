package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.AsistenciaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/asistencia")
public class AsistenciaController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("No hay ninguna sesión activa.");
        }
        if ("admin".equalsIgnoreCase(principal.getName())) {
            Usuario adminFicticio = new Usuario();
            adminFicticio.setUsername("admin");
            adminFicticio.setRol("JEFE");
            return adminFicticio;
        }
        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    // =========================================================================
    // VER ASISTENCIAS
    // =========================================================================
    @GetMapping
    public String verAsistencias(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return "redirect:/operaciones/auditoria";
    }

    // =========================================================================
    // PASE DE LISTA GENERAL (Exclusivo para JEFE / GERENTE)
    // =========================================================================
    @PostMapping("/pase-lista")
    public String registrarPaseLista(@RequestParam("usuarioId") Long usuarioId,
                                     @RequestParam("estado") String estado,
                                     @RequestParam(value = "observaciones", required = false) String observaciones,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {

        String paginaOrigen = request.getHeader("Referer");
        String redireccionDestino = (paginaOrigen != null) ? "redirect:" + paginaOrigen : "redirect:/operaciones/trabajadores";

        // 1. Validar que quien ejecuta sea un Jefe/Gerente
        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("JEFE") || a.getAuthority().equals("ROLE_JEFE") 
                            || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!esJefe) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Acceso denegado: Solo el Jefe o Gerente puede registrar asistencia.");
            return redireccionDestino;
        }

        try {
            // 2. Buscar al empleado al que se le pasa lista
            Usuario empleado = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado."));

            LocalDate hoy = LocalDate.now();
            Optional<Asistencia> existencia = asistenciaRepository.findByUsuarioAndFecha(empleado, hoy);

            Asistencia asistencia;
            if (existencia.isPresent()) {
                asistencia = existencia.get();
            } else {
                asistencia = new Asistencia();
                asistencia.setUsuario(empleado);
                asistencia.setFecha(hoy);
                asistencia.setHoraEntrada(LocalTime.now());
            }

            // 3. Guardar estado (ASISTENCIA, RETARDO, FALTA, JUSTIFICADO)
            asistencia.setEstado(estado);
            if (observaciones != null) {
                asistencia.setObservaciones(observaciones);
            }

            asistenciaRepository.save(asistencia);
            redirectAttributes.addFlashAttribute("exitoAsistencia", "Asistencia de " + empleado.getUsername() + " registrada correctamente.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Error al procesar el pase de lista: " + e.getMessage());
        }

        return redireccionDestino;
    }
}