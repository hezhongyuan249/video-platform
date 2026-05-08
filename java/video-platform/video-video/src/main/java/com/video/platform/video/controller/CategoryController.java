package com.video.platform.video.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.video.platform.common.result.Result;
import com.video.platform.video.entity.Category;
import com.video.platform.video.mapper.CategoryMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryMapper categoryMapper;

    // 获取分类列表
    @GetMapping("/list")
    public Result getCategoryList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        return Result.success(categoryMapper.selectList(wrapper));
    }

    // 新增分类
    @PostMapping("/add")
    public Result addCategory(@RequestBody Category category) {
        if (!StringUtils.hasText(category.getName()) || !StringUtils.hasText(category.getValue())) {
            return Result.error("分类名称和值不能为空");
        }

        // 检查分类值是否已存在
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getValue, category.getValue());
        if (categoryMapper.selectOne(wrapper) != null) {
            return Result.error("分类值已存在");
        }

        // 获取最大sort值并+1
        wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Category::getSort).last("LIMIT 1");
        Category maxCategory = categoryMapper.selectOne(wrapper);
        int newSort = maxCategory == null || maxCategory.getSort() == null ? 1 : maxCategory.getSort() + 1;
        category.setSort(newSort);
        
        category.setCreateTime(new Date());
        categoryMapper.insert(category);
        return Result.success();
    }

    // 更新分类
    @PostMapping("/update")
    public Result updateCategory(@RequestBody Category category) {
        if (category.getId() == null) {
            return Result.error("分类ID不能为空");
        }

        if (!StringUtils.hasText(category.getName()) || !StringUtils.hasText(category.getValue())) {
            return Result.error("分类名称和值不能为空");
        }

        // 检查分类值是否已存在（排除当前分类）
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getValue, category.getValue()).ne(Category::getId, category.getId());
        if (categoryMapper.selectOne(wrapper) != null) {
            return Result.error("分类值已存在");
        }

        categoryMapper.updateById(category);
        return Result.success();
    }

    // 删除分类
    @DeleteMapping("/delete/{id}")
    public Result deleteCategory(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.success();
    }

    // 调整分类顺序
    @PostMapping("/reorder")
    public Result reorderCategory(@RequestBody List<Category> categories) {
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            LambdaUpdateWrapper<Category> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Category::getId, category.getId()).set(Category::getSort, i + 1);
            categoryMapper.update(null, wrapper);
        }
        return Result.success();
    }
}