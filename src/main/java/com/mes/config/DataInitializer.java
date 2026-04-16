package com.mes.config;

import com.mes.entity.*;
import com.mes.repository.*;
import com.mes.util.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MaterialCategoryRepository materialCategoryRepository;
    private final MaterialRepository materialRepository;
    private final BomRepository bomRepository;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           MaterialCategoryRepository materialCategoryRepository,
                           MaterialRepository materialRepository,
                           BomRepository bomRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.materialCategoryRepository = materialCategoryRepository;
        this.materialRepository = materialRepository;
        this.bomRepository = bomRepository;
    }

    @Override
    public void run(String... args) {
        Permission userManage = createPermissionIfNotExists("user:manage", "用户管理", Permission.PermissionType.MENU);
        Permission userAdd = createPermissionIfNotExists("user:add", "添加用户", Permission.PermissionType.BUTTON);
        Permission userEdit = createPermissionIfNotExists("user:edit", "编辑用户", Permission.PermissionType.BUTTON);
        Permission userDelete = createPermissionIfNotExists("user:delete", "删除用户", Permission.PermissionType.BUTTON);
        Permission userResetPassword = createPermissionIfNotExists("user:resetPassword", "重置密码", Permission.PermissionType.BUTTON);
        
        Permission roleManage = createPermissionIfNotExists("role:manage", "角色管理", Permission.PermissionType.MENU);
        Permission roleAdd = createPermissionIfNotExists("role:add", "添加角色", Permission.PermissionType.BUTTON);
        Permission roleEdit = createPermissionIfNotExists("role:edit", "编辑角色", Permission.PermissionType.BUTTON);
        Permission roleDelete = createPermissionIfNotExists("role:delete", "删除角色", Permission.PermissionType.BUTTON);
        
        Permission permissionManage = createPermissionIfNotExists("permission:manage", "权限管理", Permission.PermissionType.MENU);
        Permission permissionAdd = createPermissionIfNotExists("permission:add", "添加权限", Permission.PermissionType.BUTTON);
        Permission permissionEdit = createPermissionIfNotExists("permission:edit", "编辑权限", Permission.PermissionType.BUTTON);
        Permission permissionDelete = createPermissionIfNotExists("permission:delete", "删除权限", Permission.PermissionType.BUTTON);
        
        Permission passwordChange = createPermissionIfNotExists("password:change", "修改密码", Permission.PermissionType.MENU);
        Permission mesView = createPermissionIfNotExists("mes:view", "查看MES数据", Permission.PermissionType.MENU);
        
        Permission uomManage = createPermissionIfNotExists("uom:manage", "计量单位管理", Permission.PermissionType.MENU);
        Permission uomAdd = createPermissionIfNotExists("uom:add", "添加计量单位", Permission.PermissionType.BUTTON);
        Permission uomEdit = createPermissionIfNotExists("uom:edit", "编辑计量单位", Permission.PermissionType.BUTTON);
        Permission uomDelete = createPermissionIfNotExists("uom:delete", "删除计量单位", Permission.PermissionType.BUTTON);

        Permission materialCategoryManage = createPermissionIfNotExists("material:category:manage", "物料分类管理", Permission.PermissionType.MENU);
        Permission materialCategoryAdd = createPermissionIfNotExists("material:category:add", "添加物料分类", Permission.PermissionType.BUTTON);
        Permission materialCategoryEdit = createPermissionIfNotExists("material:category:edit", "编辑物料分类", Permission.PermissionType.BUTTON);
        Permission materialCategoryDelete = createPermissionIfNotExists("material:category:delete", "删除物料分类", Permission.PermissionType.BUTTON);

        Permission materialManage = createPermissionIfNotExists("material:manage", "物料产品管理", Permission.PermissionType.MENU);
        Permission materialAdd = createPermissionIfNotExists("material:add", "添加物料产品", Permission.PermissionType.BUTTON);
        Permission materialEdit = createPermissionIfNotExists("material:edit", "编辑物料产品", Permission.PermissionType.BUTTON);
        Permission materialDelete = createPermissionIfNotExists("material:delete", "删除物料产品", Permission.PermissionType.BUTTON);
        Permission materialBomAdd = createPermissionIfNotExists("material:bom:add", "添加BOM组成", Permission.PermissionType.BUTTON);
        Permission materialBomDelete = createPermissionIfNotExists("material:bom:delete", "删除BOM组成", Permission.PermissionType.BUTTON);

        Role adminRole = createRoleIfNotExists("ADMIN", "系统管理员",
                userManage, userAdd, userEdit, userDelete, userResetPassword,
                roleManage, roleAdd, roleEdit, roleDelete,
                permissionManage, permissionAdd, permissionEdit, permissionDelete,
                passwordChange, mesView,
                uomManage, uomAdd, uomEdit, uomDelete,
                materialCategoryManage, materialCategoryAdd, materialCategoryEdit, materialCategoryDelete,
                materialManage, materialAdd, materialEdit, materialDelete,
                materialBomAdd, materialBomDelete);

        Role userRole = createRoleIfNotExists("USER", "普通用户",
                passwordChange, mesView);

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(PasswordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(PasswordEncoder.encode("user123"));
            user.setRealName("普通用户");
            user.setRoles(new HashSet<>(Arrays.asList(userRole)));
            userRepository.save(user);
        }

        initSampleData();
    }

    private void initSampleData() {
        if (materialCategoryRepository.count() == 0) {
            MaterialCategory rawMaterial = createCategory("原材料", null, true);
            MaterialCategory semiFinished = createCategory("半成品", null, true);
            MaterialCategory finished = createCategory("产成品", null, true);

            MaterialCategory metal = createCategory("金属材料", rawMaterial.getId(), true);
            MaterialCategory plastic = createCategory("塑料材料", rawMaterial.getId(), true);

            MaterialCategory pvc = createCategory("PVC颗粒", plastic.getId(), true);
            MaterialCategory steel = createCategory("螺纹钢", metal.getId(), true);

            Material screwdriverHead = createMaterial("MAT000001", "螺丝刀刀头", "个", steel.getId(), new BigDecimal("100"), new BigDecimal("1000"));
            Material screwdriverHandle = createMaterial("MAT000002", "螺丝刀刀柄", "个", pvc.getId(), new BigDecimal("100"), new BigDecimal("1000"));
            Material screwdriver = createMaterial("MAT000003", "螺丝刀", "个", finished.getId(), new BigDecimal("50"), new BigDecimal("500"));
            Material pvcGranule = createMaterial("MAT000004", "PVC颗粒", "kg", pvc.getId(), new BigDecimal("500"), new BigDecimal("5000"));
            Material rebar = createMaterial("MAT000005", "螺纹钢", "kg", steel.getId(), new BigDecimal("200"), new BigDecimal("2000"));

            createBom(screwdriver.getId(), screwdriverHead.getId(), new BigDecimal("1"));
            createBom(screwdriver.getId(), screwdriverHandle.getId(), new BigDecimal("1"));
            createBom(screwdriverHead.getId(), rebar.getId(), new BigDecimal("0.1"));
            createBom(screwdriverHandle.getId(), pvcGranule.getId(), new BigDecimal("0.05"));
        }
    }

    private MaterialCategory createCategory(String name, Long parentId, boolean enabled) {
        MaterialCategory category = new MaterialCategory();
        category.setName(name);
        category.setParentId(parentId);
        category.setEnabled(enabled);
        return materialCategoryRepository.save(category);
    }

    private Material createMaterial(String code, String name, String unit, Long categoryId, BigDecimal minStock, BigDecimal maxStock) {
        Material material = new Material();
        material.setCode(code);
        material.setName(name);
        material.setUnit(unit);
        material.setCategoryId(categoryId);
        material.setMinStock(minStock);
        material.setMaxStock(maxStock);
        material.setEnabled(true);
        return materialRepository.save(material);
    }

    private void createBom(Long parentId, Long childId, BigDecimal quantity) {
        Bom bom = new Bom();
        bom.setParentMaterialId(parentId);
        bom.setChildMaterialId(childId);
        bom.setQuantity(quantity);
        bomRepository.save(bom);
    }

    private Permission createPermissionIfNotExists(String name, String description, Permission.PermissionType type) {
        return permissionRepository.findByName(name)
                .map(perm -> {
                    perm.setType(type);
                    return permissionRepository.save(perm);
                })
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName(name);
                    permission.setDescription(description);
                    permission.setType(type);
                    return permissionRepository.save(permission);
                });
    }

    private Role createRoleIfNotExists(String name, String description, Permission... permissions) {
        return roleRepository.findByName(name)
                .map(role -> {
                    role.setPermissions(new HashSet<>(Arrays.asList(permissions)));
                    return roleRepository.save(role);
                })
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    role.setPermissions(new HashSet<>(Arrays.asList(permissions)));
                    return roleRepository.save(role);
                });
    }
}
