---
name: "page-style-replicator"
description: "高精度复刻页面样式，分析目标页面的布局、样式和交互，生成可复用的代码。当用户需要复制或模仿现有页面的样式时调用。"
---

# 页面样式复刻工具

## 功能描述

此技能用于高精度复刻页面样式，分析目标页面的布局、样式和交互，生成可复用的代码。它可以帮助用户快速复制现有页面的设计，确保新页面与原页面在视觉上保持一致。

## 使用场景

- 当用户需要复制现有页面的样式时
- 当用户需要模仿某个网页的设计时
- 当用户需要确保不同页面之间的视觉一致性时
- 当用户需要快速创建与现有页面风格一致的新页面时

## 操作步骤

1. **分析目标页面**：分析目标页面的HTML结构、CSS样式和JavaScript交互
2. **提取样式信息**：提取颜色、字体、间距、布局等样式信息
3. **生成代码**：根据提取的信息生成可复用的HTML、CSS和JavaScript代码
4. **优化代码**：优化生成的代码，确保其可维护性和性能
5. **应用到新项目**：将生成的代码应用到新项目中

## 技术实现

1. **HTML结构分析**：分析目标页面的DOM结构，提取语义化标签和布局结构
2. **CSS样式提取**：提取目标页面的CSS样式，包括颜色、字体、间距、边框等
3. **JavaScript交互分析**：分析目标页面的JavaScript交互，提取事件处理和动画效果
4. **响应式设计**：确保生成的代码支持响应式设计，适应不同屏幕尺寸
5. **性能优化**：优化生成的代码，减少冗余和不必要的代码

## 示例

### 输入
```
帮我复刻这个页面的样式：https://example.com
```

### 输出
```
我已经分析了目标页面的样式，以下是复刻的代码：

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Example Page</title>
  <style>
    /* 提取的CSS样式 */
    body {
      font-family: Arial, sans-serif;
      margin: 0;
      padding: 0;
      background-color: #f5f5f5;
    }
    
    .header {
      background-color: #333;
      color: white;
      padding: 20px;
      text-align: center;
    }
    
    .content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }
    
    .card {
      background-color: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      padding: 20px;
      margin-bottom: 20px;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Example Page</h1>
  </div>
  <div class="content">
    <div class="card">
      <h2>Welcome</h2>
      <p>This is an example page.</p>
    </div>
  </div>
</body>
</html>
```

您可以根据需要调整代码，确保它符合您的项目要求。
```

## 注意事项

- 复刻页面样式时，请注意版权问题，确保您有权利使用目标页面的设计
- 生成的代码可能需要根据您的项目结构进行调整
- 对于复杂的页面，可能需要手动调整生成的代码以确保其正确性
- 建议在使用生成的代码前进行测试，确保其在不同浏览器和设备上的兼容性
