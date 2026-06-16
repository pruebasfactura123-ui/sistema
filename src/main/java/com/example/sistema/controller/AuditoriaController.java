package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Auditoria;
import com.example.sistema.model.Usuario;
import com.example.sistema.model.Empresa;
import com.example.sistema.repository.AsistenciaRepository;
import com.example.sistema.repository.AuditoriaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operaciones")
public class AuditoriaController {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) throw new RuntimeException("No hay ninguna sesión activa.");
        
        // Salvavidas para el admin en memoria que no está en la BD
        if ("admin".equalsIgnoreCase(principal.getName())) {
            Usuario adminFicticio = new Usuario();
            adminFicticio.setUsername("admin");
            adminFicticio.setRol("JEFE");
            
            // 🏢 ASIGNACIÓN DE EMPRESA SIMULADA:
            // Le damos una empresa al admin en memoria para que no arrastre basura de otras empresas.
            Empresa empresaSimulada = new Empresa();
            empresaSimulada.setId(1L); // <--- CAMBIA ESTE '1' por el ID de la empresa que quieres auditar en SmarterASP
            adminFicticio.setEmpresa(empresaSimulada);
            
            return adminFicticio;
        }

        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    @GetMapping("/auditoria")
    public String mostrarAuditoria(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarioLogueado", logueado);

            String rol = logueado.getRol() != null ? logueado.getRol().toUpperCase() : "";
            if ("JEFE".equals(rol) || "GERENTE".equals(rol)) {
                
                List<Auditoria> logsFiscales;
                
                // ==================== CORRECCIÓN: FILTRADO ESTRICTO DE AUDITORÍA ====================
                if (logueado.getEmpresa() == null) {
                    logsFiscales = new ArrayList<>();
                } else {
                    Long empresaId = logueado.getEmpresa().getId();
                    // Jalar ESTRICTAMENTE las auditorías que corresponden al ID de tu empresa activa
                    logsFiscales = auditoriaRepository.findByEmpresaIdOrderByFechaRegistroDesc(empresaId);
                }
                model.addAttribute("auditorias", logsFiscales);

                // 2. Cargar asistencias filtradas estrictamente
                List<Asistencia> listaAsistencias;
                if (logueado.getEmpresa() == null) {
                    // Si por algún motivo un usuario no tiene empresa asignada, lista vacía por seguridad
                    listaAsistencias = new ArrayList<>();
                } else {
                    Long empresaId = logueado.getEmpresa().getId();
                    listaAsistencias = asistenciaRepository.findAll().stream()
                            .filter(a -> a.getUsuario() != null && a.getUsuario().getEmpresa() != null)
                            .filter(a -> a.getUsuario().getEmpresa().getId().equals(empresaId))
                            .collect(Collectors.toList());
                }
                model.addAttribute("asistencias", listaAsistencias);
                
                // Colocar dinámicamente la Razón Social en la vista (Mismo estándar de Nóminas)
                String nombreEmpresa = (logueado.getEmpresa() != null && logueado.getEmpresa().getRazonSocial() != null) 
                        ? logueado.getEmpresa().getRazonSocial() : "OFICINA FISCAL";
                model.addAttribute("empresaNombre", nombreEmpresa);
                
            } else {
                // Listas vacías si es un empleado común por seguridad
                model.addAttribute("auditorias", new ArrayList<>());
                model.addAttribute("asistencias", new ArrayList<>());
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("auditorias", new ArrayList<>());
            model.addAttribute("asistencias", new ArrayList<>());
        }

        return "auditoria";
    }
}