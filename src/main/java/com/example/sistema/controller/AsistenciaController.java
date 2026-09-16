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
    public String registrarPaseLista(@RequestParam("usuarioId") List<Long> usuarioIds,
                                     @RequestParam("estado") List<String> estados,
                                     @RequestParam(value = "observaciones", required = false) List<String> observaciones,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {

        String paginaOrigen = request.getHeader("Referer");
        String redireccionDestino = (paginaOrigen != null) ? "redirect:" + paginaOrigen : "redirect:/usuarios";

        // 1. Validar que quien ejecuta sea un Jefe/Gerente
        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("JEFE") || a.getAuthority().equals("ROLE_JEFE") 
                            || a.getAuthority().equals("GERENTE") || a.getAuthority().equals("ROLE_GERENTE")
                            || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!esJefe) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Acceso denegado: Solo el Jefe o Gerente puede registrar asistencia.");
            return redireccionDestino;
        }

        try {
            String usuarioLogueado = authentication.getName();
            int procesados = 0;

            // 2. Recorrer la lista enviada desde la tabla HTML
            for (int i = 0; i < usuarioIds.size(); i++) {
                Long uId = usuarioIds.get(i);
                String estadoStr = estados.get(i);
                String obsStr = (observaciones != null && i < observaciones.size()) ? observaciones.get(i) : "";

                Usuario empleado = usuarioRepository.findById(uId).orElse(null);

                // Regla de negocio: Ignorar al usuario activo (no se hace pase de lista a sí mismo)
                if (empleado == null || empleado.getUsername().equalsIgnoreCase(usuarioLogueado)) {
                    continue; 
                }

                LocalDate hoy = LocalDate.now();
                Optional<Asistencia> existencia = asistenciaRepository.findByUsuarioAndFecha(empleado, hoy);

                Asistencia asistencia = existencia.orElseGet(() -> {
                    Asistencia nueva = new Asistencia();
                    nueva.setUsuario(empleado);
                    nueva.setFecha(hoy);
                    nueva.setHoraEntrada(LocalTime.now());
                    return nueva;
                });

                asistencia.setEstado(estadoStr);
                asistencia.setObservaciones(obsStr);

                asistenciaRepository.save(asistencia);
                procesados++;
            }

            redirectAttributes.addFlashAttribute("exitoAsistencia", "Pase de lista registrado para " + procesados + " empleados.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Error al procesar el pase de lista: " + e.getMessage());
        }

        return redireccionDestino;
    }
}