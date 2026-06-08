package com.example.sistema.repository;

import com.example.sistema.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    
    // NUEVO: Recupera solo los usuarios/trabajadores vinculados a una empresa específica
    List<Usuario> findByEmpresaId(Long empresaId);
}