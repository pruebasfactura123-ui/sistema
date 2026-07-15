package com.example.sistema.controller;

import com.example.sistema.model.Usuario;
import com.example.sistema.repository.UsuarioRepository; // Importamos tu repositorio
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UsuarioRepository usuarioRepository; // Inyección directa al repositorio

    @ModelAttribute
    public void agregarEmpresaAlModelo(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                String username = authentication.getName();
                
                // Evitamos broncas si estás logueado con el usuario "admin" harcodeado
                if ("admin".equalsIgnoreCase(username)) {
                    model.addAttribute("empresaNombre", "SISTEMA ADMINISTRADOR");
                    return;
                }

                // Buscamos al usuario directamente en la base de datos usando el repositorio
                Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
                
                if (usuario != null && usuario.getEmpresa() != null) {
                    model.addAttribute("empresaNombre", usuario.getEmpresa().getRazonSocial());
                } else {
                    model.addAttribute("empresaNombre", "OFICINA FISCAL");
                }
            } catch (Exception e) {
                // Si algo truena en la consulta, ponemos un mensaje seguro
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        }
    }
}