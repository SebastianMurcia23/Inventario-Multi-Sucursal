package com.inventario.config;

import com.inventario.model.*;
import com.inventario.model.enums.RolUsuario;
import com.inventario.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Configuración para inicializar datos de prueba en la base de datos.
 * Solo inserta datos si las tablas están vacías.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase(
            SucursalRepository sucursalRepository,
            UsuarioRepository usuarioRepository,
            CategoriaRepository categoriaRepository,
            UnidadMedidaRepository unidadMedidaRepository,
            ProductoRepository productoRepository,
            ProveedorRepository proveedorRepository) {

        return args -> {
            // Solo insertar si no hay datos
            if (sucursalRepository.count() == 0) {
                log.info("Inicializando datos de prueba en la base de datos...");

                // Crear sucursales
                Sucursal central = sucursalRepository.save(Sucursal.builder()
                        .nombre("CENTRAL")
                        .codigo("CENTRAL")
                        .direccion("Calle Principal 123, Piso 10, Centro")
                        .ciudad("Ciudad Central")
                        .telefono("(555) 1001-1000")
                        .correo("central@optiplan.com")
                        .estado(true)
                        .build());

                Sucursal norte = sucursalRepository.save(Sucursal.builder()
                        .nombre("SUCURSAL_NORTE")
                        .codigo("NORTE")
                        .direccion("Avenida Norte 456, Zona Industrial")
                        .ciudad("Ciudad Norte")
                        .telefono("(555) 2002-2000")
                        .correo("norte@optiplan.com")
                        .estado(true)
                        .build());

                Sucursal sur = sucursalRepository.save(Sucursal.builder()
                        .nombre("SUCURSAL_SUR")
                        .codigo("SUR")
                        .direccion("Carrera Sur 789, Comercial")
                        .ciudad("Ciudad Sur")
                        .telefono("(555) 3003-3000")
                        .correo("sur@optiplan.com")
                        .estado(true)
                        .build());

                log.info("Sucursales creadas: {}", sucursalRepository.count());

                // Crear usuarios
                usuarioRepository.save(Usuario.builder()
                        .sucursal(central)
                        .nombre("Admin User")
                        .correo("admin@optiplan.com")
                        .contrasenaHash(passwordEncoder.encode("Admin123!"))
                        .rol(RolUsuario.ADMIN)
                        .activo(true)
                        .build());

                usuarioRepository.save(Usuario.builder()
                        .sucursal(central)
                        .nombre("Gerente Central")
                        .correo("gerente@sucursal.com")
                        .contrasenaHash(passwordEncoder.encode("Gerente123!"))
                        .rol(RolUsuario.GERENTE)
                        .activo(true)
                        .build());

                usuarioRepository.save(Usuario.builder()
                        .sucursal(norte)
                        .nombre("Operador Inventario")
                        .correo("operador@sucursal.com")
                        .contrasenaHash(passwordEncoder.encode("Operador123!"))
                        .rol(RolUsuario.OPERADOR)
                        .activo(true)
                        .build());

                log.info("Usuarios creados: {}", usuarioRepository.count());

                // Crear categorías
                Categoria frutas = categoriaRepository.save(Categoria.builder()
                        .nombre("FRUTAS")
                        .descripcion("Frutas frescas y tropicales")
                        .build());

                Categoria vegetales = categoriaRepository.save(Categoria.builder()
                        .nombre("VEGETALES")
                        .descripcion("Vegetales frescos para consumo humano")
                        .build());

                Categoria condimentos = categoriaRepository.save(Categoria.builder()
                        .nombre("CONDIMENTOS")
                        .descripcion("Especias y condimentos diversos")
                        .build());

                Categoria bebidas = categoriaRepository.save(Categoria.builder()
                        .nombre("BEBIDAS")
                        .descripcion("Bebidas sin alcohol")
                        .build());

                Categoria lacteos = categoriaRepository.save(Categoria.builder()
                        .nombre("LACTEOS")
                        .descripcion("Productos lácteos frescos")
                        .build());

                log.info("Categorías creadas: {}", categoriaRepository.count());

                // Crear unidades de medida
                UnidadMedida kg = unidadMedidaRepository.save(UnidadMedida.builder()
                        .nombre("KILOGRAMOS")
                        .abreviatura("KG")
                        .build());

                UnidadMedida litros = unidadMedidaRepository.save(UnidadMedida.builder()
                        .nombre("LITROS")
                        .abreviatura("L")
                        .build());

                UnidadMedida unidades = unidadMedidaRepository.save(UnidadMedida.builder()
                        .nombre("UNIDADES")
                        .abreviatura("UND")
                        .build());

                UnidadMedida cajas = unidadMedidaRepository.save(UnidadMedida.builder()
                        .nombre("CAJAS")
                        .abreviatura("CJ")
                        .build());

                log.info("Unidades de medida creadas: {}", unidadMedidaRepository.count());

                // Crear productos
                productoRepository.save(Producto.builder()
                        .sku("FRU-MAN-001")
                        .nombre("Manzana Roja")
                        .descripcion("Manzanas rojas frescas de temporada")
                        .categoria(frutas)
                        .stockMinimo(50)
                        .stockMaximo(500)
                        .activo(true)
                        .build());

                productoRepository.save(Producto.builder()
                        .sku("FRU-BAN-001")
                        .nombre("Banano")
                        .descripcion("Bananos maduros premium")
                        .categoria(frutas)
                        .stockMinimo(100)
                        .stockMaximo(1000)
                        .activo(true)
                        .build());

                productoRepository.save(Producto.builder()
                        .sku("VEG-TOM-001")
                        .nombre("Tomate")
                        .descripcion("Tomates frescos para ensalada")
                        .categoria(vegetales)
                        .stockMinimo(30)
                        .stockMaximo(300)
                        .activo(true)
                        .build());

                productoRepository.save(Producto.builder()
                        .sku("VEG-ZAN-001")
                        .nombre("Zanahoria")
                        .descripcion("Zanahorias frescas")
                        .categoria(vegetales)
                        .stockMinimo(40)
                        .stockMaximo(400)
                        .activo(true)
                        .build());

                productoRepository.save(Producto.builder()
                        .sku("LAC-LEC-001")
                        .nombre("Leche Entera")
                        .descripcion("Leche entera pasteurizada 1L")
                        .categoria(lacteos)
                        .stockMinimo(100)
                        .stockMaximo(500)
                        .activo(true)
                        .build());

                log.info("Productos creados: {}", productoRepository.count());

                // Crear proveedores
                proveedorRepository.save(Proveedor.builder()
                        .nombre("Frutas del Valle S.A.")
                        .nit("900123456-1")
                        .contacto("Juan Pérez")
                        .telefono("(555) 100-1001")
                        .correo("ventas@frutasdelvalle.com")
                        .condicionPago("30 días")
                        .activo(true)
                        .build());

                proveedorRepository.save(Proveedor.builder()
                        .nombre("Vegetales Frescos Ltda.")
                        .nit("900234567-2")
                        .contacto("María García")
                        .telefono("(555) 200-2002")
                        .correo("pedidos@vegetalesfrescos.com")
                        .condicionPago("15 días")
                        .activo(true)
                        .build());

                proveedorRepository.save(Proveedor.builder()
                        .nombre("Lácteos Premium")
                        .nit("900345678-3")
                        .contacto("Carlos Rodríguez")
                        .telefono("(555) 300-3003")
                        .correo("ventas@lacteospremium.com")
                        .condicionPago("Contado")
                        .activo(true)
                        .build());

                log.info("Proveedores creados: {}", proveedorRepository.count());

                log.info("=== Datos de prueba inicializados correctamente ===");
                log.info("Usuario admin: admin@optiplan.com / Admin123!");
                log.info("Usuario gerente: gerente@sucursal.com / Gerente123!");
                log.info("Usuario operador: operador@sucursal.com / Operador123!");

            } else {
                log.info("Base de datos ya contiene datos, omitiendo inicialización");
            }
        };
    }
}
