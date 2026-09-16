package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.AsistenciaRepository;
import com.example.sistema.repository.UsuarioRepository;
import com.example.sistema.repository.AuditoriaRepository; // Asegúrate de tener este import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operaciones/auditoria")
public class AsistenciaController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private AuditoriaRepository auditoriaRepository;

    // =========================================================================
    // VISTA DE AUDITORÍA Y ASISTENCIAS AGRUPADAS POR SEMANAS
    // =========================================================================
    @GetMapping
    public String verAuditoriaYAsistencias(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        // 1. Cargar auditorías
        if (auditoriaRepository != null) {
            model.addAttribute("auditorias", auditoriaRepository.findAll());
        }

        // 2. Cargar asistencias y agruparlas por semanas descendentemente
        List<Asistencia> listaAsistencias = asistenciaRepository.findAll();

        Map<String, List<Asistencia>> asistenciasPorSemana = listaAsistencias.stream()
                .filter(a -> a.getFecha() != null)
                .sorted(Comparator.comparing(Asistencia::getFecha).reversed())
                .collect(Collectors.groupingBy(
                        Asistencia::getEtiquetaSemana,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("asistenciasPorSemana", asistenciasPorSemana);

        return "auditoria";
    }

    // =========================================================================
    // PASE DE LISTA GENERAL (Exclusivo para JEFE / GERENTE)
    // =========================================================================
    @PostMapping("/pase-lista")
    public String registrarPaseLista(@RequestParam(value = "usuarioId", required = false) List<Long> usuarioIds,
                                     @RequestParam(value = "estado", required = false) List<String> estados,
                                     @RequestParam(value = "observaciones", required = false) List<String> observaciones,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {

        String paginaOrigen = request.getHeader("Referer");
        String redireccionDestino = (paginaOrigen != null && !paginaOrigen.contains("/asistencia/pase-lista")) 
                ? "redirect:" + paginaOrigen 
                : "redirect:/usuarios";

        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("JEFE") || a.getAuthority().equals("ROLE_JEFE") 
                            || a.getAuthority().equals("GERENTE") || a.getAuthority().equals("ROLE_GERENTE")
                            || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!esJefe) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Acceso denegado: Solo el Jefe o Gerente puede registrar asistencia.");
            return redireccionDestino;
        }

        if (usuarioIds == null || usuarioIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "No hay empleados disponibles para pasar lista.");
            return redireccionDestino;
        }

        try {
            String usuarioLogueado = authentication.getName();
            int procesados = 0;

            for (int i = 0; i < usuarioIds.size(); i++) {
                Long uId = usuarioIds.get(i);
                String estadoStr = (estados != null && i < estados.size()) ? estados.get(i) : "ASISTENCIA";
                String obsStr = (observaciones != null && i < observaciones.size()) ? observaciones.get(i) : "";

                Usuario empleado = usuarioRepository.findById(uId).orElse(null);

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

            redirectAttributes.addFlashAttribute("exitoAsistencia", "¡Pase de lista guardado con éxito! Se registraron " + procesados + " asistencias.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Error al procesar el pase de lista: " + e.getMessage());
        }

        return redireccionDestino;
    }
}