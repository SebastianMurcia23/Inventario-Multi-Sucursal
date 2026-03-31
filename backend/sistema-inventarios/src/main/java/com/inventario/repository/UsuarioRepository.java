package com.inventario.repository;

import com.inventario.model.Usuario;
import com.inventario.model.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findBySucursalIdSucursal(Integer idSucursal);

    List<Usuario> findBySucursalIdSucursalAndActivoTrue(Integer idSucursal);

    List<Usuario> findByRol(RolUsuario rol);
}
