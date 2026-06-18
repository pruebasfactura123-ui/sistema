package com.example.sistema.repository;

import com.example.sistema.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    
    // 1. Traer todas las facturas aisladas por Empresa (Trae INGRESO y EGRESO juntos)
    List<Factura> findByEmpresaId(Long empresaId);
    
    // 2. Filtrar por fechas asegurando el aislamiento por Empresa (Trae ambos tipos en ese rango)
    List<Factura> findByEmpresaIdAndFechaBetween(Long empresaId, LocalDate fechaInicio, LocalDate fechaFin);

    // =========================================================================
    // MÉTODOS PARA SEPARAR COMPROBANTES DE FACTURAS EMITIDAS (AUDITORÍA VS EMISIÓN)
    // =========================================================================

    // Para el Dashboard: Devuelve todos los registros con archivo
    List<Factura> findByEmpresaIdAndNombreArchivoNotAndNombreArchivoIsNotNull(Long empresaId, String nombreArchivo);
    
    List<Factura> findByEmpresaIdAndNombreArchivoNotAndNombreArchivoIsNotNullAndFechaBetween(Long empresaId, String nombreArchivo, LocalDate inicio, LocalDate fin);

    // CORRECCIÓN HISTORIAL: Trae estrictamente las facturas generadas en el sistema (cuyo archivo empiece con 'manual_')
    @Query("SELECT f FROM Factura f WHERE f.empresa.id = :empresaId AND f.nombreArchivo LIKE 'manual_%'")
    List<Factura> findFacturasManualesPorEmpresa(@Param("empresaId") Long empresaId);
}