package com.example.sistema.repository;

import com.example.sistema.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    // Trae el historial ordenado del más nuevo al más viejo (Útil para Administradores globales)
    List<Auditoria> findAllByOrderByFechaRegistroDesc();

    // ==================== NUEVO: FILTRADO MULTIEMPRESA ====================
    // Recupera la bitácora vinculada únicamente a la empresa del usuario en sesión
    List<Auditoria> findByEmpresaIdOrderByFechaRegistroDesc(Long empresaId);
}