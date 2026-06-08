package com.example.sistema.controller;

import com.example.sistema.model.Empresa;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.EmpresaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@Controller
public class EmpresaController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
            return "redirect:/?errorSesion";
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
}