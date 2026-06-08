package com.example.sistema.repository;

import com.example.sistema.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // Consulta explícita y segura que usaremos en ambos controladores
    @Query("SELECT c FROM Cliente c WHERE c.empresa.id = :empresaId")
    List<Cliente> findClientesPorEmpresa(@Param("empresaId") Long empresaId);
}