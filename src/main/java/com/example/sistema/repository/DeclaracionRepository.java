package com.example.sistema.repository;

import com.example.sistema.model.Declaracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeclaracionRepository extends JpaRepository<Declaracion, Long> {
    
    // Buscar todas las declaraciones de una sola empresa ordenadas por año y mes
    List<Declaracion> findByEmpresaIdOrderByAnioDescMesDesc(Long empresaId);

    // Comprobar si ya existe una declaración con el mismo año, mes y tipo para esa empresa
    boolean existsByEmpresaIdAndAnioAndMesAndTipo(Long empresaId, Integer anio, String mes, String tipo);
}