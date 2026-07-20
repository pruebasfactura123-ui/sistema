package com.example.sistema.controller;

import com.example.sistema.model.Usuario;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @ModelAttribute
    public void agregarEmpresaAlModelo(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                String username = authentication.getName();

                // Si entras con el admin VIP
                if ("admin".equalsIgnoreCase(username)) {
                    model.addAttribute("empresaNombre", "SISTEMA ADMINISTRADOR");
                    return;
                }

                // Busca al trabajador o jefe en la BD por su username
                Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

                if (usuario != null && usuario.getEmpresa() != null) {
                    // Inyecta la Razón Social real de la empresa del trabajador
                    model.addAttribute("empresaNombre", usuario.getEmpresa().getRazonSocial());
                } else {
                    model.addAttribute("empresaNombre", "OFICINA FISCAL");
                }
            } catch (Exception e) {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        }
    }
}