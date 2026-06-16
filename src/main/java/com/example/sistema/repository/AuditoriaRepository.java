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
    // Busca los registros que pertenezcan a la empresa O que no tengan empresa asignada (NULL)
List<Auditoria> findByEmpresaIdOrEmpresaIsNullOrderByFechaRegistroDesc(Long empresaId);
}

