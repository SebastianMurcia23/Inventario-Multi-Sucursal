package com.inventario.service;

import com.inventario.dto.request.ProductoCreateRequest;
import com.inventario.dto.request.ProductoUpdateRequest;
import com.inventario.dto.response.ProductoResponse;
import com.inventario.model.Categoria;
import com.inventario.model.Producto;
import com.inventario.repository.CategoriaRepository;
import com.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar productos del sistema.
 * Maneja operaciones CRUD de productos con validaciones de categoría y unidad de medida.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Obtiene todos los productos activos del sistema.
     *
     * @return Lista de ProductoResponse con todos los productos activos
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProducts() {
        log.debug("Obteniendo todos los productos activos");

        List<Producto> productos = productoRepository.findByActivoTrue();

        log.info("Se obtuvieron {} productos activos", productos.size());

        return productos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un producto específico por su ID.
     *
     * @param id ID del producto
     * @return ProductoResponse con los datos del producto
     * @throws RuntimeException si el producto no existe
     */
    @Transactional(readOnly = true)
    public ProductoResponse getProductById(Integer id) {
        log.debug("Obteniendo producto con ID: {}", id);

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado con ID: {}", id);
                    return new RuntimeException("Producto no encontrado con ID: " + id);
                });

        log.info("Producto obtenido exitosamente: {} (ID: {})", producto.getNombre(), id);

        return toResponse(producto);
    }

    /**
     * Crea un nuevo producto en el sistema.
     * Valida que la categoría exista y que el stock mínimo sea válido.
     *
     * @param request Datos del nuevo producto (ProductoCreateRequest)
     * @return ProductoResponse con los datos del producto creado
     * @throws RuntimeException si el SKU está duplicado o la categoría no existe
     */
    @Transactional
    public ProductoResponse createProduct(ProductoCreateRequest request) {
        log.info("Iniciando creación de nuevo producto: {}", request.nombre());

        // Validar que no exista producto con el mismo SKU
        if (productoRepository.existsBySku(request.sku())) {
            log.warn("Intento de crear producto con SKU duplicado: {}", request.sku());
            throw new RuntimeException("Ya existe un producto con el SKU: " + request.sku());
        }

        // Validar que la categoría existe
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada con ID: {}", request.categoriaId());
                    return new RuntimeException("Categoría no encontrada con ID: " + request.categoriaId());
                });

        // Validar stock mínimo
        if (request.stockMinimo() < 0) {
            log.warn("Intento de crear producto con stock mínimo negativo: {}", request.stockMinimo());
            throw new RuntimeException("El stock mínimo no puede ser negativo");
        }

        // Crear entidad Producto
        Producto producto = Producto.builder()
                .sku(request.sku())
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .categoria(categoria)
                .stockMinimo(request.stockMinimo())
                .stockMaximo(request.stockMaximo())
                .activo(true)
                .build();

        Producto productoGuardado = productoRepository.save(producto);

        log.info("Producto creado exitosamente con ID: {} - Nombre: {} - SKU: {}",
                 productoGuardado.getIdProducto(), productoGuardado.getNombre(),
                 productoGuardado.getSku());

        return toResponse(productoGuardado);
    }

    /**
     * Actualiza un producto existente.
     * No permite cambiar categoría (campo inmutable).
     *
     * @param id ID del producto a actualizar
     * @param request Datos actualizados del producto
     * @return ProductoResponse con los datos actualizados
     * @throws RuntimeException si el producto no existe
     */
    @Transactional
    public ProductoResponse updateProduct(Integer id, ProductoUpdateRequest request) {
        log.info("Iniciando actualización de producto ID: {}", id);

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado para actualizar con ID: {}", id);
                    return new RuntimeException("Producto no encontrado con ID: " + id);
                });

        // Validar stock mínimo
        if (request.stockMinimo() < 0) {
            log.warn("Intento de actualizar producto con stock mínimo negativo: {}", request.stockMinimo());
            throw new RuntimeException("El stock mínimo no puede ser negativo");
        }

        // Actualizar campos permitidos
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setStockMinimo(request.stockMinimo());
        producto.setStockMaximo(request.stockMaximo());
        producto.setActivo(request.activo());

        Producto productoActualizado = productoRepository.save(producto);

        log.info("Producto actualizado exitosamente - ID: {} - Nombre: {}",
                 id, productoActualizado.getNombre());

        return toResponse(productoActualizado);
    }

    /**
     * Desactiva un producto del sistema.
     * Los productos desactivados no aparecen en búsquedas pero se conservan en inventario.
     *
     * @param id ID del producto a eliminar (desactivar)
     * @throws RuntimeException si el producto no existe
     */
    @Transactional
    public void deleteProduct(Integer id) {
        log.info("Iniciando eliminación (desactivación) de producto ID: {}", id);

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado para eliminar con ID: {}", id);
                    return new RuntimeException("Producto no encontrado con ID: " + id);
                });

        // Desactivar el producto en lugar de borrarlo
        producto.setActivo(false);
        productoRepository.save(producto);

        log.info("Producto desactivado exitosamente - ID: {} - Nombre: {}",
                 id, producto.getNombre());
    }

    /**
     * Busca productos por nombre o descripción.
     * La búsqueda es insensible a mayúsculas/minúsculas.
     *
     * @param keyword Palabra clave para buscar en nombre del producto
     * @return Lista de ProductoResponse con los productos coincidentes
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> searchProducts(String keyword) {
        log.debug("Buscando productos con palabra clave: {}", keyword);

        List<Producto> productos = productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(keyword);

        log.info("Se encontraron {} productos con la palabra clave: {}", productos.size(), keyword);

        return productos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Producto a ProductoResponse.
     *
     * @param producto Entidad Producto
     * @return ProductoResponse con los datos del producto
     */
    private ProductoResponse toResponse(Producto producto) {
        String categoriaNombre = producto.getCategoria() != null ?
                producto.getCategoria().getNombre() : "Sin categoría";

        return new ProductoResponse(
                producto.getIdProducto(),
                producto.getNombre(),
                producto.getSku(),
                producto.getDescripcion(),
                categoriaNombre,
                producto.getStockMinimo(),
                producto.getStockMaximo(),
                producto.getActivo()
        );
    }
}
