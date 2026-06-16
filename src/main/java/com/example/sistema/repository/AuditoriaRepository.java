package com.example.sistema.repository;

import com.example.sistema.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    // Trae el historial ordenado del más nuevo al más viejo (Útil para Administradores globales)
    List<Auditoria> findAllByOrderByFechaRegistroDesc();

    // ==================== SOLUCIÓN: FILTRADO ESTRICTO MULTIEMPRESA ====================
    // Cambiamos el método para que busque ÚNICAMENTE por el ID de la empresa (sin meter los NULL viejos)
    List<Auditoria> findByEmpresaIdOrderByFechaRegistroDesc(Long empresaId);
}