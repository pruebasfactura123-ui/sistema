package com.example.sistema.controller;

import com.example.sistema.model.Cliente;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.ClienteRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Cargar la página con la lista filtrada por la empresa del usuario logueado
    @GetMapping("/clientes")
    public String listarClientes(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        
        try {
            // 1. Obtener los datos de la empresa del usuario en sesión
            Usuario logueado = usuarioRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            if (logueado.getEmpresa() == null) {
                throw new RuntimeException("El usuario no tiene una empresa asignada.");
            }
            
            Long empresaId = logueado.getEmpresa().getId();

            // CORRECCIÓN CLAVE: Usamos 'Cliente' con mayúscula y el método seguro 'findClientesPorEmpresa'
            List<Cliente> listaClientes = clienteRepository.findClientesPorEmpresa(empresaId);
            if (listaClientes == null) {
                listaClientes = new ArrayList<>(); // Si viene null de la BD, inicializamos una lista limpia
            }
            
            model.addAttribute("clientes", listaClientes);
            model.addAttribute("clienteNuevo", new Cliente()); 
            
            // Mantener el nombre de la empresa arriba en el diseño
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
            model.addAttribute("usuarioLogueado", logueado);

        } catch (Exception e) {
            e.printStackTrace();
            // En lugar de mandar una pantalla blanca de error 500, mandamos listas seguras para que pinte la interfaz vacía
            model.addAttribute("clientes", new ArrayList<Cliente>());
            model.addAttribute("clienteNuevo", new Cliente());
            model.addAttribute("empresaNombre", "ERROR SISTEMA");
            return "clientes";
        }

        return "clientes"; 
    }

    // Guardar nuevo cliente ASOCIANDO su empresa
    @PostMapping("/clientes/guardar")
    public String guardarCliente(Principal principal, @ModelAttribute("clienteNuevo") Cliente cliente) {
        if (principal == null) return "redirect:/login";

        try {
            // 1. Buscar al usuario que está guardando el registro
            Usuario logueado = usuarioRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 2. ASIGNARLE la empresa del usuario al cliente para evitar el campo NULL
            cliente.setEmpresa(logueado.getEmpresa());

            // 3. Guardar con la relación establecida
            clienteRepository.save(cliente);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/clientes?error";
        }

        return "redirect:/clientes?exito";
    }
}