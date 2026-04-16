package com.mes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "material_category")
@ToString(exclude = {"children", "parent"})
public class MaterialCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private MaterialCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<MaterialCategory> children = new ArrayList<>();

    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_enabled")
    private boolean enabled = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (children == null) {
            children = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialCategory)) return false;
        MaterialCategory that = (MaterialCategory) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
