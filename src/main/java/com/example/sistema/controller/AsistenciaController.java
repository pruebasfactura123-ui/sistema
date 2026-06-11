package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.AsistenciaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
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
        // Salvavidas para el admin en memoria
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
    // VER ASISTENCIAS: Redirige al panel unificado de pestañas (auditoria.html)
    // =========================================================================
    @GetMapping
    public String verAsistencias(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        // En lugar de buscar datos aquí, mandamos al usuario a la ruta correcta que maneja AuditoriaController
        return "redirect:/operaciones/auditoria";
    }

    // =========================================================================
    // REGISTRO DE ENTRADA (Mantiene tu regla: El Jefe NO registra)
    // =========================================================================
    @PostMapping("/entrada")
    public String registrarEntrada(Principal principal, Authentication authentication, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        String paginaOrigen = request.getHeader("Referer");
        String redireccionDestino = (paginaOrigen != null) ? "redirect:" + paginaOrigen : "redirect:/operaciones/auditoria";

        if (principal == null || authentication == null) {
            return "redirect:/login";
        }

        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("JEFE") || a.getAuthority().equals("ROLE_JEFE"));

        if (esJefe) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Los usuarios con rol de Jefe no requieren registrar asistencia.");
            return redireccionDestino;
        }

        try {
            Usuario usuario = getUsuarioLogueado(principal);
            LocalDate hoy = LocalDate.now();
            Optional<Asistencia> existencia = asistenciaRepository.findByUsuarioAndFecha(usuario, hoy);

            if (existencia.isPresent()) {
                redirectAttributes.addFlashAttribute("alertaAsistencia", "Ya has registrado tu entrada el día de hoy.");
            } else {
                Asistencia asistencia = new Asistencia();
                asistencia.setUsuario(usuario);
                asistencia.setFecha(hoy);
                asistencia.setHoraEntrada(LocalTime.now());
                asistenciaRepository.save(asistencia);
                
                String horaFormateada = LocalTime.now().toString().substring(0, 5);
                redirectAttributes.addFlashAttribute("exitoAsistencia", "¡Éxito! Tu entrada ha sido registrada a las " + horaFormateada + ".");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Error al guardar asistencia: " + e.getMessage());
        }

        return redireccionDestino;
    }

    // =========================================================================
    // REGISTRO DE SALIDA (Mantiene tu regla: El Jefe NO registra)
    // =========================================================================
    @PostMapping("/salida")
    public String registrarSalida(Principal principal, Authentication authentication, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        String paginaOrigen = request.getHeader("Referer");
        String redireccionDestino = (paginaOrigen != null) ? "redirect:" + paginaOrigen : "redirect:/operaciones/auditoria";

        if (principal == null || authentication == null) {
            return "redirect:/login";
        }

        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("JEFE") || a.getAuthority().equals("ROLE_JEFE"));

        if (esJefe) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Los usuarios con rol de Jefe no requieren registrar asistencia.");
            return redireccionDestino;
        }

        try {
            Usuario usuario = getUsuarioLogueado(principal);
            LocalDate hoy = LocalDate.now();
            Optional<Asistencia> existencia = asistenciaRepository.findByUsuarioAndFecha(usuario, hoy);

            if (existencia.isPresent()) {
                Asistencia asistencia = existencia.get();
                if (asistencia.getHoraSalida() != null) {
                    redirectAttributes.addFlashAttribute("alertaAsistencia", "Ya has registrado tu salida el día de hoy.");
                } else {
                    asistencia.setHoraSalida(LocalTime.now());
                    asistenciaRepository.save(asistencia);
                    
                    String horaFormateada = LocalTime.now().toString().substring(0, 5);
                    redirectAttributes.addFlashAttribute("exitoAsistencia", "¡Éxito! Salida registrada a las " + horaFormateada + ". ¡Buen descanso!");
                }
            } else {
                redirectAttributes.addFlashAttribute("alertaAsistencia", "Primero debes registrar tu hora de entrada.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Error al actualizar salida: " + e.getMessage());
        }

        return redireccionDestino;
    }
}