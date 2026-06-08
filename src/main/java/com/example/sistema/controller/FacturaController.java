package com.example.sistema.controller;

import com.example.sistema.model.Empresa;
import com.example.sistema.model.Factura;
import com.example.sistema.model.Usuario;
import com.example.sistema.model.Cliente;
import com.example.sistema.repository.EmpresaRepository;
import com.example.sistema.repository.FacturaRepository;
import com.example.sistema.repository.UsuarioRepository;
import com.example.sistema.repository.ClienteRepository;
import com.example.sistema.repository.AuditoriaRepository;
import com.example.sistema.model.Auditoria;
import com.fasterxml.jackson.databind.ObjectMapper;

// IMPORTS ESTRUCTURADOS DE OPENPDF
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;

import io.facturapi.Facturapi;
import io.facturapi.models.Invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.*;
import java.awt.Color;

@Controller
public class FacturaController {

    @Autowired private FacturaRepository facturaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditoriaRepository auditoriaRepository;

    private final String apiKeyFacturapi = "sk_test_tG9FGgQXufEjgoQ5DobnVawKTiiNxiTXFMQady6aTT";
    private static final String BASE_PATH = System.getProperty("java.io.tmpdir") + "/facturas_xml/";

    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) throw new RuntimeException("No hay ninguna sesión activa.");
        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/registrar-empresa")
    public String vistaRegistro() { return "registrar-empresa"; }

    @PostMapping("/registrar-empresa")
    public String registrarEmpresaYJefe(@RequestParam String razonSocial, @RequestParam String rfc, @RequestParam String username, @RequestParam String password) {
        try {
            Empresa emp = new Empresa();
            emp.setRazonSocial(razonSocial.trim());
            emp.setRfc(rfc.trim().toUpperCase());
            Empresa empresaGuardada = empresaRepository.save(emp);
            
            Usuario jefe = new Usuario();
            jefe.setUsername(username.trim());
            jefe.setPassword(passwordEncoder.encode(password.trim()));
            jefe.setRol("JEFE"); 
            jefe.setEmpresa(empresaGuardada);
            jefe.setFotoUrl("https://cdn-icons-png.flaticon.com/512/3135/3135715.png");
            
            usuarioRepository.save(jefe);
            return "redirect:/login?empresaCreada";
        } catch (Exception e) { e.printStackTrace(); return "redirect:/registrar-empresa?error"; }
    }

    @GetMapping("/")
    public String inicio(Principal principal, 
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio, 
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin, 
                         @RequestParam(required = false) String errorPermisoFactura,
                         Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Long idEmpresa = logueado.getEmpresa().getId();
            
            List<Factura> todasLasFacturas = (fechaInicio != null && fechaFin != null) 
                    ? facturaRepository.findByEmpresaIdAndNombreArchivoNotAndNombreArchivoIsNotNullAndFechaBetween(idEmpresa, "", fechaInicio, fechaFin) 
                    : facturaRepository.findByEmpresaIdAndNombreArchivoNotAndNombreArchivoIsNotNull(idEmpresa, "");
                    
            if (todasLasFacturas == null) todasLasFacturas = new ArrayList<>();
            
            // FILTRO ESTRICTO: El Dashboard PRINCIPAL solo debe mostrar COMPROBANTES XML subidos (NO facturas manuales)
            List<Factura> comprobantesXml = todasLasFacturas.stream()
                    .filter(f -> f.getNombreArchivo() != null && !f.getNombreArchivo().startsWith("manual_"))
                    .collect(Collectors.toList());
            
            double ingresos = comprobantesXml.stream().filter(f -> "INGRESO".equalsIgnoreCase(f.getTipo())).mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0).sum();
            double egresos = comprobantesXml.stream().filter(f -> "EGRESO".equalsIgnoreCase(f.getTipo())).mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0).sum();
            
            double[] ingresosMeses = new double[12];
            double[] egresosMeses = new double[12];

            for (Factura f : comprobantesXml) {
                if (f.getFecha() != null) {
                    int mesIndex = f.getFecha().getMonthValue() - 1;
                    if ("INGRESO".equalsIgnoreCase(f.getTipo())) {
                        ingresosMeses[mesIndex] += (f.getTotal() != null ? f.getTotal() : 0.0);
                    } else if ("EGRESO".equalsIgnoreCase(f.getTipo())) {
                        egresosMeses[mesIndex] += (f.getTotal() != null ? f.getTotal() : 0.0);
                    }
                }
            }

            model.addAttribute("datosIngresos", ingresosMeses);
            model.addAttribute("datosEgresos", egresosMeses);

            if (errorPermisoFactura != null) {
                model.addAttribute("errorPermisoFactura", true);
            }

            model.addAttribute("usuarioLogueado", logueado);
            model.addAttribute("facturas", comprobantesXml); // Solo se envían comprobantes XML
            model.addAttribute("subtotalTotal", ingresos);
            model.addAttribute("ivaTrasladado", ingresos * 0.16);
            model.addAttribute("totalNeto", ingresos - egresos);
            model.addAttribute("xmlProcesados", comprobantesXml.size());
            
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        } catch (Exception e) { model.addAttribute("facturas", new ArrayList<>()); }
        return "index";
    }

    @GetMapping("/facturas/nueva")
    public String nuevaFactura(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarioLogueado", logueado);
            Long empresaId = logueado.getEmpresa().getId();
            
            List<Cliente> listaClientes = clienteRepository.findClientesPorEmpresa(empresaId);
            if (listaClientes == null) {
                listaClientes = new ArrayList<>();
            }
            
            model.addAttribute("clientes", listaClientes);
            model.addAttribute("empresaRfc", logueado.getEmpresa().getRfc());
            
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("clientes", new ArrayList<>());
            model.addAttribute("empresaNombre", "ERROR SYSTEMA");
        }
        return "nueva-factura";
    }

    @PostMapping("/facturas/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarFacturaManual(
            @RequestParam("rfcCliente") String rfcCliente,
            @RequestParam("nombreCliente") String nombreCliente,
            @RequestParam("subtotal") Double subtotal,
            @RequestParam("iva") Double iva,
            @RequestParam("total") Double total,
            @RequestParam("tipo") String tipo,
            @RequestParam("metodoPago") String metodoPago,
            @RequestParam("formaPago") String formaPago,
            @RequestParam("conceptosJson") String conceptosJson,
            Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Facturapi facturapiApp = new Facturapi(this.apiKeyFacturapi);

            Map<String, Object> facturaMap = new HashMap<>();
            String metodoLimpio = metodoPago.contains("-") ? metodoPago.split("-")[0].trim() : metodoPago.trim();
            String formaLimpia = formaPago.contains("-") ? formaPago.split("-")[0].trim() : formaPago.trim();
            
            facturaMap.put("payment_form", formaLimpia);   
            facturaMap.put("payment_method", metodoLimpio); 
            facturaMap.put("use", "G03");                     

            Map<String, Object> customer = new HashMap<>();
            Map<String, String> address = new HashMap<>();
            
            String rfcLimpio = rfcCliente.trim().toUpperCase();
            customer.put("tax_id", rfcLimpio);
            customer.put("legal_name", nombreCliente.trim().toUpperCase());
            
            if ("EKU9003173C9".equals(rfcLimpio)) {
                customer.put("tax_system", "601"); 
                address.put("zip", "23000"); 
            } else if ("XAXX010101000".equals(rfcLimpio) || "XEXX010101000".equals(rfcLimpio)) {
                customer.put("tax_system", "616"); 
                address.put("zip", "06470"); 
            } else {
                customer.put("tax_system", "601"); 
                address.put("zip", "06470"); 
            }
            
            customer.put("address", address);
            facturaMap.put("customer", customer);

            ObjectMapper mapper = new ObjectMapper();
            List<?> conceptosListaFrontend = mapper.readValue(conceptosJson, List.class);
            List<Map<String, Object>> itemsList = new ArrayList<>();

            for(Object obj : conceptosListaFrontend) {
                if (obj instanceof Map) {
                    Map<?, ?> cFront = (Map<?, ?>) obj;
                    Map<String, Object> item = new HashMap<>();
                    Map<String, Object> product = new HashMap<>();
                    
                    String descripcion = "Servicio General";
                    if(cFront.containsKey("descripcion")) descripcion = cFront.get("descripcion").toString();
                    
                    double precioUnitario = cFront.containsKey("precioUnitario") ? Double.parseDouble(cFront.get("precioUnitario").toString()) : 100.00;
                    int cantidad = cFront.containsKey("cantidad") ? Integer.parseInt(cFront.get("cantidad").toString()) : 1;

                    product.put("description", descripcion.trim());
                    product.put("price", precioUnitario);
                    product.put("product_key", "84111500"); 
                    product.put("unit_key", "E48");        
                    
                    item.put("quantity", cantidad);
                    item.put("product", product);
                    itemsList.add(item);
                }
            }
            facturaMap.put("items", itemsList);

            String idFacturapi = null;
            String uuidCfdi = UUID.randomUUID().toString(); 
            byte[] xmlBytes = null;
            byte[] pdfBytes = null; 

            try {
                Invoice respuestaInvoice = facturapiApp.invoices().create(facturaMap, null);
                idFacturapi = respuestaInvoice.getId();
                if (respuestaInvoice.getUuid() != null) {
                    uuidCfdi = respuestaInvoice.getUuid();
                }
                xmlBytes = facturapiApp.invoices().downloadXml(idFacturapi);
                pdfBytes = facturapiApp.invoices().downloadPdf(idFacturapi); 
            } catch (Exception e) {
                System.out.println("[SISTEMA] Modo pruebas / Excepción de Facturapi controlada.");
            }

            String nombreXmlNuevo = "manual_" + uuidCfdi + ".xml";
            String nombrePdfNuevo = "manual_" + uuidCfdi + ".pdf";
            String rutaDirectorio = BASE_PATH + logueado.getEmpresa().getId() + "/";
            File dir = new File(rutaDirectorio);
            if (!dir.exists()) dir.mkdirs();
            
            if (xmlBytes != null) {
                Files.write(Paths.get(rutaDirectorio + nombreXmlNuevo), xmlBytes); 
            }
            if (pdfBytes != null) {
                Files.write(Paths.get(rutaDirectorio + nombrePdfNuevo), pdfBytes); 
            }

            Factura factura = new Factura();
            factura.setRfcEmisor(logueado.getEmpresa().getRfc());
            factura.setRfcCliente(rfcLimpio);
            factura.setSubtotal(subtotal);
            factura.setIva(iva);
            factura.setTotal(total);
            factura.setFecha(LocalDate.now());
            factura.setTipo(tipo);
            factura.setNombreArchivo(nombreXmlNuevo); 
            factura.setEstado("TIMBRADA"); 
            factura.setEmpresa(logueado.getEmpresa());

            facturaRepository.save(factura);

            // AUDITORÍA: CREACIÓN MANUAL
            String usuarioActivo = (principal != null) ? principal.getName() : "Sistema";
            String detalles = "Creó factura manual (" + tipo + ") Folio: " + uuidCfdi 
                            + " para el Cliente: " + rfcLimpio + " por un Total de $" + String.format("%.2f", total);
            Auditoria registro = new Auditoria(usuarioActivo, "CREAR FACTURA MANUAL", detalles);
            auditoriaRepository.save(registro);

            return ResponseEntity.ok().body("{\"status\":\"success\",\"uuid\":\"" + uuidCfdi + "\",\"id\":" + factura.getId() + "}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/facturas/obtener/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerFacturaPorId(@PathVariable Long id, Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Optional<Factura> facturaOpt = facturaRepository.findById(id);
            
            if (facturaOpt.isPresent()) {
                Factura f = facturaOpt.get();
                if (f.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", f.getId());
                    response.put("rfcCliente", f.getRfcCliente());
                    response.put("total", f.getTotal());
                    response.put("tipo", f.getTipo());
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.status(404).body("{\"message\":\"Factura no encontrada\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/facturas/editar")
    public String actualizarFactura(
            @RequestParam("id") Long id,
            @RequestParam("rfcCliente") String rfcCliente,
            @RequestParam("total") Double total,
            @RequestParam("tipo") String tipo,
            Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            facturaRepository.findById(id).ifPresent(f -> {
                if (f.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                    f.setRfcCliente(rfcCliente.trim());
                    f.setTotal(total);
                    f.setTipo(tipo);
                    facturaRepository.save(f);

                    // AUDITORÍA: MODIFICACIÓN
                    String usuarioActivo = (principal != null) ? principal.getName() : "Sistema";
                    String detalles = "Modificó datos de la factura ID: " + id 
                                    + ". Nuevo Cliente: " + rfcCliente.trim() + ", Nuevo Total: $" + String.format("%.2f", total);
                    Auditoria registro = new Auditoria(usuarioActivo, "MODIFICAR FACTURA", detalles);
                    auditoriaRepository.save(registro);
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/facturas/historial";
    }

    @GetMapping("/facturas/historial")
    public String historialFacturas(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Long idEmpresa = logueado.getEmpresa().getId();
            
            // FILTRO ESTRICTO: El Historial de Facturas MANUALES solo debe traer las que empiezan con "manual_"
            List<Factura> todas = facturaRepository.findByEmpresaIdAndNombreArchivoNotAndNombreArchivoIsNotNull(idEmpresa, "");
            List<Factura> facturasManuales = todas.stream()
                    .filter(f -> f.getNombreArchivo() != null && f.getNombreArchivo().startsWith("manual_"))
                    .collect(Collectors.toList());
            
            model.addAttribute("usuarioLogueado", logueado);
            model.addAttribute("facturas", facturasManuales);
            
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("facturas", new ArrayList<>());
            model.addAttribute("empresaNombre", "ERROR SYSTEMA");
        }
        return "historial-facturas";
    }

    @PostMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(Principal principal, @PathVariable Long id) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            if ("JEFE".equalsIgnoreCase(logueado.getRol()) || "GERENTE".equalsIgnoreCase(logueado.getRol())) {
                clienteRepository.findById(id).ifPresent(c -> {
                    if (c.getEmpresa() != null && c.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                        
                        String usuarioActivo = (principal != null) ? principal.getName() : "Sistema";
                        String detalles = "Eliminó al Cliente: " + c.getNombre() + " (RFC: " + c.getRfc() + ") con ID: " + id;
                        Auditoria registro = new Auditoria(usuarioActivo, "ELIMINAR", detalles);
                        auditoriaRepository.save(registro);

                        clienteRepository.deleteById(id);
                    }
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/clientes";
    }

    @GetMapping("/usuarios")
    public String listarTrabajadores(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarios", usuarioRepository.findByEmpresaId(logueado.getEmpresa().getId()));
            model.addAttribute("usuarioLogueado", logueado); 
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        } catch (Exception e) { model.addAttribute("usuarios", new ArrayList<>()); }
        return "usuarios";
    }

    @PostMapping("/usuarios/crear")
    public String crearTrabajador(Principal principal, @RequestParam String nuevoUsuario, @RequestParam String nuevaClave, @RequestParam(required = false) String fotoUrl) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Usuario u = new Usuario();
            u.setUsername(nuevoUsuario.trim());
            u.setPassword(passwordEncoder.encode(nuevaClave.trim()));
            u.setRol(nuevoUsuario.trim().toLowerCase().startsWith("gerente") ? "GERENTE" : "TRABAJADOR");
            u.setEmpresa(logueado.getEmpresa());
            if (fotoUrl == null || fotoUrl.trim().isEmpty()) {
                u.setFotoUrl("https://cdn-icons-png.flaticon.com/512/3135/3135715.png");
            } else {
                u.setFotoUrl(fotoUrl.trim());
            }
            usuarioRepository.save(u);
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminarTrabajador(Principal principal, @PathVariable Long id) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                usuarioRepository.findById(id).ifPresent(u -> {
                    if ("JEFE".equalsIgnoreCase(u.getRol())) return;
                    if (u.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                        
                        String usuarioActivo = (principal != null) ? principal.getName() : "Sistema";
                        String detalles = "Eliminó al Trabajador/Usuario: '" + u.getUsername() + "' (Rol: " + u.getRol() + ") con ID: " + id;
                        Auditoria registro = new Auditoria(usuarioActivo, "ELIMINAR", detalles);
                        auditoriaRepository.save(registro);

                        usuarioRepository.deleteById(id);
                    }
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/cambiar-rol/{id}")
    public String cambiarRol(Principal principal, @PathVariable Long id, @RequestParam String accion) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                usuarioRepository.findById(id).ifPresent(u -> {
                    if ("JEFE".equalsIgnoreCase(u.getRol())) return;
                    if (u.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                        if ("subir".equalsIgnoreCase(accion)) u.setRol("GERENTE");
                        else if ("bajar".equalsIgnoreCase(accion)) u.setRol("TRABAJADOR");
                        usuarioRepository.save(u);
                    }
                });
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/perfil")
    public String verPerfil(Model model, Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuario", logueado);
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        } catch (Exception e) { return "redirect:/"; }
        return "perfil";
    }

    @PostMapping("/perfil/actualizar-foto")
    public String actualizarFoto(@RequestParam String nuevaFotoUrl, Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            if (nuevaFotoUrl != null && !nuevaFotoUrl.trim().isEmpty()) {
                logueado.setFotoUrl(nuevaFotoUrl.trim());
            } else {
                logueado.setFotoUrl("https://cdn-icons-png.flaticon.com/512/3135/3135715.png");
            }
            usuarioRepository.save(logueado);
            return "redirect:/perfil?fotoExito";
        } catch (Exception e) { return "redirect:/perfil?error"; }
    }

    @PostMapping("/perfil/actualizar-clave")
    public String actualizarClave(@RequestParam String nuevaClave, Principal principal) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            logueado.setPassword(passwordEncoder.encode(nuevaClave.trim()));
            usuarioRepository.save(logueado);
            return "redirect:/perfil?exito";
        } catch (Exception e) { return "redirect:/perfil?error"; }
    }

    @PostMapping("/subir")
    public String subir(Principal principal, @RequestParam("archivo") MultipartFile[] archivos) {
        Usuario logueado = getUsuarioLogueado(principal);
        String ruta = BASE_PATH + logueado.getEmpresa().getId() + "/";
        new File(ruta).mkdirs();
        for (MultipartFile archivo : archivos) {
            try {
                archivo.transferTo(Paths.get(ruta + archivo.getOriginalFilename()));
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                
                org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(archivo.getInputStream());
                
                Factura f = new Factura();
                org.w3c.dom.Element comp = (org.w3c.dom.Element) doc.getElementsByTagNameNS("*", "Comprobante").item(0);
                if (comp != null) {
                    f.setTotal(Double.parseDouble(comp.getAttribute("Total")));
                    f.setFecha(LocalDate.parse(comp.getAttribute("Fecha").substring(0, 10)));
                    f.setTipo("E".equals(comp.getAttribute("TipoDeComprobante")) ? "EGRESO" : "INGRESO");
                    
                    org.w3c.dom.NodeList receptor = doc.getElementsByTagNameNS("*", "Receptor");
                    if (receptor.getLength() > 0) {
                        org.w3c.dom.Element recElem = (org.w3c.dom.Element) receptor.item(0);
                        f.setRfcCliente(recElem.getAttribute("Nombre"));
                    }
                    
                    f.setNombreArchivo(archivo.getOriginalFilename());
                    f.setEmpresa(logueado.getEmpresa());
                    facturaRepository.save(f);

                    String usuarioActivo = (principal != null) ? principal.getName() : "Sistema";
                    String detalles = "Cargó archivo XML al servidor: '" + archivo.getOriginalFilename() 
                                    + "' vinculando una factura de tipo " + f.getTipo() + " por $" + String.format("%.2f", f.getTotal());
                    Auditoria registro = new Auditoria(usuarioActivo, "CARGAR XML", detalles);
                    auditoriaRepository.save(registro);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return "redirect:/";
    }

    @GetMapping("/descargar/{identificador}")
    @ResponseBody
    public ResponseEntity<Resource> descargar(Principal principal, @PathVariable String identificador) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            String rutaCarpeta = BASE_PATH + logueado.getEmpresa().getId() + "/";
            String nombreArchivo = "factura_" + identificador + ".xml";

            File carpeta = new File(rutaCarpeta);
            if (!carpeta.exists()) { carpeta.mkdirs(); }

            Path path = Paths.get(rutaCarpeta + nombreArchivo);

            String xmlFalso = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<cfdi:Comprobante version=\"4.0\" mensaje=\"Simulacion local Sandbox\"/>";
            Files.write(path, Collections.singletonList(xmlFalso));

            Resource resource = new FileSystemResource(path.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/descargar-pdf/{identificador}")
    @ResponseBody
    public ResponseEntity<Resource> descargarPdf(Principal principal, @PathVariable String identificador) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            String idEmpresaCarpeta = String.valueOf(logueado.getEmpresa().getId());
            String nombreArchivo = "factura_" + identificador + ".pdf";
            Path path = Paths.get(BASE_PATH, idEmpresaCarpeta, nombreArchivo);
            
            Files.createDirectories(path.getParent());
            
            String rfcEmisor = logueado.getEmpresa().getRfc();
            String razonSocial = logueado.getEmpresa().getRazonSocial();
            String rfcCliente = "XAXX010101000";
            double total = 0.00;
            double subtotal = 0.00;
            double iva = 0.00;
            String tipo = "INGRESO";
            String fecha = LocalDate.now().toString();

            if (identificador.matches("^\\d+$")) {
                Optional<Factura> fOpt = facturaRepository.findById(Long.parseLong(identificador));
                if (fOpt.isPresent()) {
                    Factura f = fOpt.get();
                    if (f.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                        rfcCliente = f.getRfcCliente() != null ? f.getRfcCliente() : rfcCliente;
                        total = f.getTotal() != null ? f.getTotal() : 0.00;
                        subtotal = f.getSubtotal() != null ? f.getSubtotal() : total;
                        iva = f.getIva() != null ? f.getIva() : 0.00;
                        tipo = f.getTipo() != null ? f.getTipo().toUpperCase() : tipo;
                        fecha = f.getFecha() != null ? f.getFecha().toString() : fecha;
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                Document document = new Document(PageSize.A4, 45, 45, 45, 45);
                PdfWriter.getInstance(document, fos);
                
                document.open();
                
                Font fontEmpresa = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(20, 40, 80));
                Font fontSeccion = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
                Font fontContenidoBold = new Font(Font.HELVETICA, 10, Font.BOLD);
                Font fontContenido = new Font(Font.HELVETICA, 10, Font.NORMAL);

                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setWidthPercentage(100);
                headerTable.setWidths(new int[]{60, 40});

                PdfPCell leftHeader = new PdfPCell();
                leftHeader.setBorder(PdfPCell.NO_BORDER);
                leftHeader.addElement(new Paragraph(razonSocial.toUpperCase(), fontEmpresa));
                leftHeader.addElement(new Paragraph("RFC EMISOR: " + rfcEmisor, fontContenidoBold));
                leftHeader.addElement(new Paragraph("Regimen Fiscal: 601 - General de Ley Personas Morales", fontContenido));
                headerTable.addCell(leftHeader);

                PdfPCell rightHeader = new PdfPCell();
                rightHeader.setBorder(PdfPCell.NO_BORDER);
                rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rightHeader.addElement(new Paragraph("FACTURA / COMPROBANTE", fontContenidoBold));
                rightHeader.addElement(new Paragraph("Folio Interno: " + identificador, fontContenido));
                rightHeader.addElement(new Paragraph("Fecha: " + fecha, fontContenido));
                rightHeader.addElement(new Paragraph("Efecto: " + tipo, fontContenidoBold));
                headerTable.addCell(rightHeader);
                
                document.add(headerTable);
                document.add(new Paragraph(" \n"));

                PdfPTable receptorBarra = new PdfPTable(1);
                receptorBarra.setWidthPercentage(100);
                PdfPCell celdaBarra = new PdfPCell(new Phrase("DATOS DEL CLIENTE (RECEPTOR)", fontSeccion));
                celdaBarra.setBackgroundColor(new Color(40, 70, 120));
                celdaBarra.setPadding(5);
                celdaBarra.setBorder(PdfPCell.NO_BORDER);
                receptorBarra.addCell(celdaBarra); 
                document.add(receptorBarra);

                PdfPTable receptorDatos = new PdfPTable(1);
                receptorDatos.setWidthPercentage(100);
                PdfPCell celdaDatos = new PdfPCell();
                celdaDatos.setPadding(8);
                celdaDatos.setBorderColor(new Color(200, 200, 200));
                celdaDatos.addElement(new Paragraph("RFC CLIENTE: " + rfcCliente, fontContenidoBold));
                celdaDatos.addElement(new Paragraph("Uso CFDI: G03 - Gastos en general", fontContenido));
                receptorDatos.addCell(celdaDatos);
                document.add(receptorDatos);
                document.add(new Paragraph(" \n"));

                PdfPTable conceptosBarra = new PdfPTable(1);
                conceptosBarra.setWidthPercentage(100);
                PdfPCell celdaBarra2 = new PdfPCell(new Phrase("DETALLE DEL COMPROBANTE", fontSeccion));
                celdaBarra2.setBackgroundColor(new Color(40, 70, 120));
                celdaBarra2.setPadding(5);
                celdaBarra2.setBorder(PdfPCell.NO_BORDER);
                conceptosBarra.addCell(celdaBarra2);
                document.add(conceptosBarra);

                PdfPTable tablaFinanciera = new PdfPTable(2);
                tablaFinanciera.setWidthPercentage(100);
                tablaFinanciera.setWidths(new int[]{70, 30});

                PdfPCell cSubLabel = new PdfPCell(new Phrase(" Subtotal Base (Moneda Nacional)", fontContenido));
                cSubLabel.setPadding(6);
                cSubLabel.setBorderColor(new Color(220, 220, 220));
                PdfPCell cSubVal = new PdfPCell(new Phrase(String.format(" $%.2f MXN", subtotal), fontContenido));
                cSubVal.setPadding(6);
                cSubVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cSubVal.setBorderColor(new Color(220, 220, 220));
                tablaFinanciera.addCell(cSubLabel);
                tablaFinanciera.addCell(cSubVal);

                PdfPCell cIvaLabel = new PdfPCell(new Phrase(" Impuesto Trasladado (IVA 16%)", fontContenido));
                cIvaLabel.setPadding(6);
                cIvaLabel.setBorderColor(new Color(220, 220, 220));
                PdfPCell cIvaVal = new PdfPCell(new Phrase(String.format(" $%.2f MXN", iva), fontContenido));
                cIvaVal.setPadding(6);
                cIvaVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cIvaVal.setBorderColor(new Color(220, 220, 220));
                tablaFinanciera.addCell(cIvaLabel);
                tablaFinanciera.addCell(cIvaVal);

                PdfPCell cTotLabel = new PdfPCell(new Phrase(" TOTAL NETO A PAGAR", fontContenidoBold));
                cTotLabel.setPadding(8);
                cTotLabel.setBackgroundColor(new Color(245, 245, 245));
                cTotLabel.setBorderColor(new Color(180, 180, 180));
                PdfPCell cTotVal = new PdfPCell(new Phrase(String.format(" $%.2f MXN", total), fontContenidoBold));
                cTotVal.setPadding(8);
                cTotVal.setBackgroundColor(new Color(245, 245, 245));
                cTotVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cTotVal.setBorderColor(new Color(180, 180, 180));
                tablaFinanciera.addCell(cTotLabel);
                tablaFinanciera.addCell(cTotVal);

                document.add(tablaFinanciera);
                document.close();
            }

            Resource resource = new FileSystemResource(path.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/borrar/{id}")
    public String borrarFactura(Principal principal, @PathVariable Long id) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            
            facturaRepository.findById(id).ifPresent(f -> {
                if (f.getEmpresa().getId().equals(logueado.getEmpresa().getId())) {
                    
                    String usuarioActivo = (principal != null) ? principal.getName() : "Sistema";
                    String detalles = "Eliminó del sistema la factura tipo: " + f.getTipo() 
                                    + " | Cliente: " + f.getRfcCliente() 
                                    + " | Monto: $" + String.format("%.2f", f.getTotal());
                    
                    Auditoria registro = new Auditoria(usuarioActivo, "ELIMINAR", detalles);
                    auditoriaRepository.save(registro);

                    if (f.getNombreArchivo() != null && !f.getNombreArchivo().isEmpty()) {
                        String idEmpresaCarpeta = String.valueOf(logueado.getEmpresa().getId());
                        Path xmlPath = Paths.get(BASE_PATH, idEmpresaCarpeta, f.getNombreArchivo());
                        
                        String nombrePdf = f.getNombreArchivo().replace(".xml", ".pdf");
                        Path pdfPath = Paths.get(BASE_PATH, idEmpresaCarpeta, nombrePdf);

                        try {
                            Files.deleteIfExists(xmlPath);
                            Files.deleteIfExists(pdfPath);
                        } catch (Exception ioException) {
                            System.err.println("Error eliminando archivos físicos: " + ioException.getMessage());
                        }
                    }
                    facturaRepository.deleteById(id);
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/facturas/historial"; 
    }

    @GetMapping("/operaciones/impuestos")
    public String calcularImpuestos(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            Long idEmpresa = logueado.getEmpresa().getId();
            
            List<Factura> todas = facturaRepository.findByEmpresaIdAndNombreArchivoNotAndNombreArchivoIsNotNull(idEmpresa, "");
            if (todas == null) todas = new ArrayList<>();
            
            // FILTRO ESTRICTO: Para impuestos y cálculos fiscales, SOLO tomamos los comprobantes XML válidos (NO manuales)
            List<Factura> comprobantesXml = todas.stream()
                    .filter(f -> f.getNombreArchivo() == null || !f.getNombreArchivo().startsWith("manual_"))
                    .collect(Collectors.toList());
            
            double ingresosSubtotal = comprobantesXml.stream()
                    .filter(f -> "INGRESO".equalsIgnoreCase(f.getTipo()))
                    .mapToDouble(f -> f.getSubtotal() != null ? f.getSubtotal() : 0.0).sum();
                    
            double ingresosIva = comprobantesXml.stream()
                    .filter(f -> "INGRESO".equalsIgnoreCase(f.getTipo()))
                    .mapToDouble(f -> f.getIva() != null ? f.getIva() : 0.0).sum();
            
            double egresosSubtotal = comprobantesXml.stream()
                    .filter(f -> "EGRESO".equalsIgnoreCase(f.getTipo()))
                    .mapToDouble(f -> f.getSubtotal() != null ? f.getSubtotal() : 0.0).sum();
                    
            double egresosIva = comprobantesXml.stream()
                    .filter(f -> "EGRESO".equalsIgnoreCase(f.getTipo()))
                    .mapToDouble(f -> f.getIva() != null ? f.getIva() : 0.0).sum();
            
            double balanceIva = ingresosIva - egresosIva;
            
            model.addAttribute("usuarioLogueado", logueado);
            model.addAttribute("ingresosSubtotal", ingresosSubtotal);
            model.addAttribute("ivaTrasladado", ingresosIva);
            model.addAttribute("egresosSubtotal", egresosSubtotal);
            model.addAttribute("ivaAcreditable", egresosIva);
            model.addAttribute("balanceIva", balanceIva);
            
            if ("JEFE".equalsIgnoreCase(logueado.getRol())) {
                model.addAttribute("empresaNombre", logueado.getEmpresa().getRazonSocial());
            } else {
                model.addAttribute("empresaNombre", "OFICINA FISCAL");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("balanceIva", 0.0);
            model.addAttribute("empresaNombre", "ERROR");
        }
        return "impuestos";
    }
}