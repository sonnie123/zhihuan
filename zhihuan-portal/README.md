# 智换 Zhihuan — AI 驱动的二手交易平台前端

> 让闲置价值重生 · 让设计毫不逊色于京东/闲鱼/转转/亚马逊

## 🚀 快速启动

```bash
cd F:\file\learn\projects\JAVA\zhihuan\zhihuan-portal
npm install      # 安装依赖（首次）
npm run dev      # 启动开发服务器 (http://localhost:5173)
```

打开浏览器访问: **http://localhost:5173**

## 📍 Phase 1 验证页面

| 路由 | 页面 | 验证重点 |
|---|---|---|
| `/` | 首页 | 玻璃拟态顶栏 + Hero 渐变 + AI 推荐 + 8 类目 + TabBar |
| `/product/1` | 商品详情 | 图集轮播 + AI 估价面板 + 卖家信用 + 评价 |
| `/publish` | AI 一键发布 | 4 步流程 + 5 节点扫描 + 打字机光标动画 |
| `/im/chat/c1` | IM 议价 | 三栏布局 + AI 议价卡价格让步轨迹 |
| `/user` | 个人中心 | 渐变 Hero + 信用分环形图 + 4 数据卡 + AI 助手 |

## 🎨 设计系统

- **主色**: 紫 `#7C3AED` → 青 `#06B6D4` 渐变
- **辅助**: 琥珀 `#F59E0B` (价格突出)
- **风格**: 玻璃拟态 + AI 科技感,与闲鱼/转转米黄差异化
- **Token**: 7 类 (color / spacing / typography / radius / shadow / motion / z-index)
- **组件库**: 12 个自研 Zh* 组件 (Button / Card / Price / Avatar / Tag / Image / Empty / Skeleton / Stepper / Bubble / Score / Toast)
- **图标**: 22 个自研 SVG 图标 (线性 + 双色)

## 🛠 技术栈

- **框架**: Vue 3.5 + Composition API + `<script setup>`
- **构建**: Vite 6 (HMR < 100ms)
- **类型**: TypeScript 5.6
- **状态**: Pinia 2 + pinia-plugin-persistedstate
- **路由**: Vue Router 4 (懒加载)
- **UI**: Element Plus 2.8 (管理后台) + Vant 4 (移动端 H5, 预留)
- **HTTP**: Axios 1.7 (mock 模式通过 adapter 拦截)
- **Mock**: 自实现 22 个端点,5 个 AI 议价剧本

## 📁 项目结构

```
src/
├── api/           适配层 (client.ts + 6 个服务模块)
├── mocks/         Mock 数据 (22 端点, 5 AI 议价剧本)
├── design/        设计系统 (token + 12 组件 + 22 图标)
├── stores/        Pinia (user/product/im/app)
├── layouts/       DefaultLayout / IMLayout
├── views/         5 个 Phase 1 门面页面
├── composables/   useAIAnimation / useCountUp / useIntersectionObserver
├── types/         5 个实体类型 (对齐后端 Dubbo DTO)
└── utils/         format / storage
```

## ✅ 验证清单 (请逐项确认)

打开每个页面,逐项检查:

### 首页 `/`
- [ ] 顶栏玻璃拟态(背景模糊 + 半透白)
- [ ] Logo "Z 智换" (紫青渐变)
- [ ] 搜索框占位 "搜索闲置好物,试试 AI 智能搜索"
- [ ] Hero 大字 "让闲置价值重生" 渐变
- [ ] CTA "立即拍照发布" (主渐变) + "体验 AI 议价" (描边)
- [ ] 3 项统计数字 (128,394 / 96.5% / ¥1.2亿)
- [ ] 右侧 AI 智能识别中卡片 (¥6,299 + iPhone 15 Pro)
- [ ] 8 个圆形类目入口 (加载后显示图标 + 文字)
- [ ] "AI 为你精选" 18 个商品卡片 (3 列 × 6 行)
- [ ] 卡片有 AI 理由徽章 (你喜欢数码 / 卖家信用 96 分 等)
- [ ] 限时秒杀区
- [ ] 关注流
- [ ] 猜你喜欢
- [ ] 底部 TabBar (中央凸起发布按钮)

### 商品详情 `/product/1`
- [ ] 主图 1:1 大图 (Unsplash iPhone)
- [ ] 缩略图横向滚动
- [ ] 渐变大号价格 (¥X,XXX)
- [ ] AI 估价徽章 "AI 估价 ¥X,XXX-XXX"
- [ ] 卖家卡片 (头像 + 信用分 + 关注)
- [ ] 商品描述 / 参数表
- [ ] 评价区
- [ ] 相似推荐
- [ ] 底部三按钮 (联系卖家 / 立即购买 / 议价)

### AI 发布 `/publish`
- [ ] 步骤指示器 4 步
- [ ] 上传区
- [ ] **点 "使用示例图片" → "开始 AI 识别"**
- [ ] **核心验证**: 5 节点依次激活 + 扫描线 + 打字机光标

### IM 议价 `/im/chat/c1`
- [ ] 三栏布局 (会话列表 / 对话 / 商品信息)
- [ ] **核心验证**: AI 议价卡的价格让步轨迹

### 个人中心 `/user`
- [ ] 渐变 Hero (头像 + 信用分环形图)
- [ ] 4 数据卡 (卖出/买入/收藏/足迹)
- [ ] AI 助手入口卡片
- [ ] 订单状态时间线
- [ ] 我的服务网格

## 🔧 后端切换

把 `.env.production` 的 `VITE_USE_MOCK` 改为 `false`,前端零代码改动即可对接真实 Gateway (http://localhost:9000)。

## 📦 实施进度

- [x] Phase 0: 项目脚手架 + 设计系统 + 12 组件 + 22 图标
- [x] **Phase 1: 5 个 C 端门面页面** (本轮交付)
- [ ] Phase 2: C 端交易主链路 10 页 (分类/搜索/购物车/结算/订单等)
- [ ] Phase 3: C 端增值 10 页
- [ ] Phase 4: IM 独立页 + 通知中心 + 帮助/协议
- [ ] Phase 5: 管理后台 12 页
- [ ] Phase 6: E2E 测试 + 性能优化 + 部署文档

## 🐛 已知问题

- Vite dev HMR 有时需要手动刷新页面
- 浏览器 fetch 在某些场景下被 mock 框架污染 (已修复为 axios adapter)

## 📞 下一步

1. 浏览器访问 http://localhost:5173 看效果
2. 逐项验证上面的清单
3. 确认品质后我开始 Round 2 (Phase 2 交易主链路 10 页)
