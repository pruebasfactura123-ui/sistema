package com.example.sistema.controller;

import com.example.sistema.model.Cliente;
import com.example.sistema.model.Usuario;
import com.example.sistema.model.Auditoria;
import com.example.sistema.repository.ClienteRepository;
import com.example.sistema.repository.UsuarioRepository;
import com.example.sistema.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) throw new RuntimeException("No hay ninguna sesión activa.");
        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    // Cargar la página con la lista filtrada por la empresa del usuario logueado
    @GetMapping("/clientes")
    public String listarClientes(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            
            if (logueado.getEmpresa() == null) {
                throw new RuntimeException("El usuario no tiene una empresa asignada.");
            }
            
            Long empresaId = logueado.getEmpresa().getId();

            List<Cliente> listaClientes = clienteRepository.findClientesPorEmpresa(empresaId);
            if (listaClientes == null) {
                listaClientes = new ArrayList<>();
            }
            
            model.addAttribute("clientes", listaClientes);
            model.addAttribute("clienteNuevo", new Cliente()); 
            
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
            model.addAttribute("usuarioLogueado", logueado);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("clientes", new ArrayList<Cliente>());
            model.addAttribute("clienteNuevo", new Cliente());
            model.addAttribute("empresaNombre", "ERROR SISTEMA");
            return "clientes";
        }

        return "clientes"; 
    }

    // 1. Guardar nuevo cliente con validación estricta de RFC y sanitización de datos
    @PostMapping("/clientes/guardar")
    public String guardarCliente(Principal principal, @ModelAttribute("clienteNuevo") Cliente cliente) {
        if (principal == null) return "redirect:/login";

        try {
            Usuario logueado = getUsuarioLogueado(principal);

            if (cliente.getNombre() == null || cliente.getRfc() == null) {
                return "redirect:/clientes?errorDatosIncompletos";
            }

            String nombreLimpio = cliente.getNombre().trim().toUpperCase();
            String rfcLimpio = cliente.getRfc().trim().toUpperCase();

            // VALIDACIÓN 1: Solo letras, espacios, acentos y puntos (S.A. de C.V.)
            if (!nombreLimpio.matches("^[A-ZÁÉÍÓÚÑ\\s\\.]+$")) {
                return "redirect:/clientes?errorNombre";
            }

            // VALIDACIÓN 2: Formato oficial del RFC en México (3-4 letras, 6 números, 3 homoclave)
            if (!rfcLimpio.matches("^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$")) {
                return "redirect:/clientes?errorRfcFormato";
            }

            // VALIDACIÓN 3: El RFC no debe repetirse dentro de la misma empresa
            boolean rfcDuplicado = clienteRepository.existsByRfcAndEmpresaId(rfcLimpio, logueado.getEmpresa().getId());
            if (rfcDuplicado) {
                return "redirect:/clientes?errorRfcDuplicado";
            }

            // Asignar los valores procesados y normalizados
            cliente.setNombre(nombreLimpio);
            cliente.setRfc(rfcLimpio);
            cliente.setEmpresa(logueado.getEmpresa());

            // Normalizar campos opcionales si vienen vacíos
            if (cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
                cliente.setTelefono("N/A");
            }
            if (cliente.getCorreo() == null || cliente.getCorreo().isBlank()) {
                cliente.setCorreo("N/A");
            }

            // Guardar cliente en BD
            clienteRepository.save(cliente);

            // REGISTRO EN BITÁCORA
            String detalles = "Agregó un nuevo cliente al sistema: " + cliente.getNombre() 
                            + " con RFC: " + cliente.getRfc();
                            
            Auditoria registro = new Auditoria(logueado.getUsername(), "CREAR CLIENTE", detalles, logueado.getEmpresa());
            auditoriaRepository.save(registro);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/clientes?error";
        }

        return "redirect:/clientes?exito";
    }

    // 2. Dar de baja clientes (Soporta mapeo @GetMapping y @RequestMapping para evitar errores 405)
    @RequestMapping(value = "/clientes/eliminar/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String eliminarCliente(Principal principal, @PathVariable Long id) {
        if (principal == null) return "redirect:/login";

        try {
            Usuario logueado = getUsuarioLogueado(principal);
            
            // Filtro de seguridad por Roles tolerantes a la baja
            if ("JEFE".equalsIgnoreCase(logueado.getRol()) || "GERENTE".equalsIgnoreCase(logueado.getRol()) || "ADMIN".equalsIgnoreCase(logueado.getRol())) {
                
                clienteRepository.findById(id).ifPresent(c -> {
                    // Verificamos propiedad de multi-tenancy (misma empresa)
                    if (c.getEmpresa() != null && c.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                        
                        // Generar la bitácora antes de eliminar de la BD
                        String detalles = "Eliminó del sistema al Cliente: " + c.getNombre() + " (RFC: " + c.getRfc() + ")";
                        Auditoria registro = new Auditoria(logueado.getUsername(), "ELIMINAR CLIENTE", detalles, logueado.getEmpresa());
                        auditoriaRepository.save(registro);

                        // Eliminación física
                        clienteRepository.deleteById(id);
                    }
                });
            } else {
                return "redirect:/clientes?errorPermisos";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/clientes?errorEliminar";
        }

        return "redirect:/clientes?exitoEliminar";
    }
}