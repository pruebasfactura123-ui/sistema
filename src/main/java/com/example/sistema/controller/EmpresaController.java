package com.example.sistema.controller;

import com.example.sistema.model.Empresa;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.EmpresaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class EmpresaController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) throw new RuntimeException("No hay ninguna sesión activa.");
        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    @GetMapping("/empresa")
    public String verEmpresa(Model model, Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Empresa empresaAsociada = logueado.getEmpresa();
            
            if (empresaAsociada == null) {
                model.addAttribute("empresa", new Empresa());
                model.addAttribute("empresaNombre", "SIN EMPRESA ASOCIADA");
            } else {
                model.addAttribute("empresa", empresaAsociada);
                model.addAttribute("empresaNombre", empresaAsociada.getRazonSocial());
            }
            
            model.addAttribute("usuarioLogueado", logueado);
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/usuarios?errorSesion";
        }
        
        return "empresa"; 
    }

    @PostMapping("/empresa/actualizar")
    public String actualizarEmpresa(@ModelAttribute Empresa empresaFormulario, Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Empresa empresaReal = logueado.getEmpresa();
            
            if (empresaReal == null) {
                return "redirect:/empresa?error=SinEmpresa";
            }
            
            empresaReal.setRazonSocial(empresaFormulario.getRazonSocial().trim());
            empresaReal.setRfc(empresaFormulario.getRfc().trim().toUpperCase());

            empresaRepository.save(empresaReal);
            
            return "redirect:/empresa?exito";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/empresa?error";
        }
    }

    // MÉTODO OPTIMIZADO: Guarda y te mantiene en la sección de trabajadores sin parpadeos extraños
    @PostMapping("/usuarios/crear")
    public String registrarPersonalAdministrativo(
            @RequestParam("nuevoUsuario") String nuevoUsuario,
            @RequestParam("nuevaClave") String nuevaClave,
            Principal principal) {
        
        if (principal == null) return "redirect:/login";

        try {
            Usuario logueado = getUsuarioLogueado(principal);
            String usuarioLimpio = nuevoUsuario.trim();

            // VALIDACIÓN 1: Que el nombre no contenga números ni caracteres especiales
            if (!usuarioLimpio.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                return "redirect:/usuarios?errorUserNombre";
            }

            // VALIDACIÓN 2: Que el username no exista ya en la base de datos (Garantiza Identificador único)
            boolean usuarioExiste = usuarioRepository.findByUsername(usuarioLimpio).isPresent();
            if (usuarioExiste) {
                return "redirect:/usuarios?errorUserDuplicado";
            }

            // Crear el nuevo usuario asignándole la misma empresa del Jefe/Gerente actual
            Usuario empleado = new Usuario();
            empleado.setUsername(usuarioLimpio);
            
            if (passwordEncoder != null) {
                empleado.setPassword(passwordEncoder.encode(nuevaClave));
            } else {
                empleado.setPassword(nuevaClave); 
            }
            
            empleado.setRol("AUXILIAR"); // Rol por defecto para el personal autorizado
            empleado.setEmpresa(logueado.getEmpresa());
            empleado.setFotoUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=100&auto=format&fit=crop"); // Avatar por defecto

            usuarioRepository.save(empleado);

            return "redirect:/usuarios?exitoUser";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/usuarios?errorUser";
        }
    }
}