import type { ProductDTO } from '@/types/entity/product'
import { getUserById } from './users'

// 真实可用的 Unsplash 图片 (商品类别)
const IMG = {
  iphone: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&h=800&fit=crop',
  macbook: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=800&fit=crop',
  airpods: 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&h=800&fit=crop',
  watch: 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=800&h=800&fit=crop',
  camera: 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop',
  book: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&h=800&fit=crop',
  bag: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=800&h=800&fit=crop',
  shoes: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop',
  dress: 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&h=800&fit=crop',
  coffee: 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=800&h=800&fit=crop',
  tent: 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800&h=800&fit=crop',
  switch: 'https://images.unsplash.com/photo-1612036782180-6f0822045d23?w=800&h=800&fit=crop',
  painting: 'https://images.unsplash.com/photo-1549887534-1541e9326642?w=800&h=800&fit=crop',
  skateboard: 'https://images.unsplash.com/photo-1547447134-cd3f5c716030?w=800&h=800&fit=crop',
  perfume: 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=800&h=800&fit=crop',
  cup: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=800&h=800&fit=crop',
}

const make = (data: Partial<ProductDTO> & { id: number; sellerId: number; title: string; price: number; coverImage: string }): ProductDTO => {
  const seller = getUserById(data.sellerId)
  const conditionText = ['', '全新', '几乎全新', '轻微使用', '明显使用痕迹'][data.condition || 2]
  return {
    images: [data.coverImage],
    viewCount: Math.floor(Math.random() * 5000) + 100,
    favoriteCount: Math.floor(Math.random() * 200) + 5,
    status: 3,
    aiAuditStatus: 1,
    publishTime: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19).replace('T', ' '),
    condition: 2,
    conditionText,
    description: data.description || '闲置出售,自用物品,非质量问题不退换,欢迎咨询。',
    categoryId: data.categoryId || 1,
    categoryName: data.categoryName || '数码',
    location: data.location || '北京 朝阳',
    tags: data.tags || ['自用', '包邮'],
    ...data,
    seller,
  } as ProductDTO
}

export const products: ProductDTO[] = [
  // 数码类
  make({ id: 1, sellerId: 1001, title: 'iPhone 15 Pro 256G 原色钛金属 9 成新', price: 6299, originalPrice: 8999, coverImage: IMG.iphone, categoryId: 1, categoryName: '数码', condition: 2, tags: ['iPhone', '自用', '原色钛金属', '9成新'], location: '北京 朝阳', description: '去年 10 月入手,一直带壳贴膜使用,无磕碰无划痕,电池效率 96%。配件齐全(原盒+原装数据线),非诚勿扰,可面交验机。' }),
  make({ id: 2, sellerId: 1001, title: 'MacBook Air M2 13.6 寸 16G/512G 星光色', price: 7299, originalPrice: 11999, coverImage: IMG.macbook, categoryId: 1, categoryName: '数码', condition: 2, tags: ['MacBook', 'M2', '设计师首选'], location: '上海 徐汇' }),
  make({ id: 3, sellerId: 1007, title: 'AirPods Pro 2 主动降噪 含原装充电盒', price: 1299, originalPrice: 1899, coverImage: IMG.airpods, categoryId: 1, categoryName: '数码', condition: 2, tags: ['AirPods', '降噪'] }),
  make({ id: 4, sellerId: 1001, title: 'Apple Watch S9 45mm GPS 版', price: 2399, originalPrice: 3199, coverImage: IMG.watch, categoryId: 1, categoryName: '数码', condition: 3, tags: ['Apple Watch', 'S9'] }),
  make({ id: 5, sellerId: 1003, title: 'Sony A7M4 机身 + 28-75 套机镜头', price: 13800, originalPrice: 18999, coverImage: IMG.camera, categoryId: 1, categoryName: '数码', condition: 2, tags: ['Sony', '微单', '摄影器材'] }),

  // 书籍类
  make({ id: 6, sellerId: 1002, title: '《三体》全集三本 大刘签名版', price: 168, originalPrice: 168, coverImage: IMG.book, categoryId: 6, categoryName: '图书', condition: 2, tags: ['三体', '科幻', '签名版'] }),
  make({ id: 7, sellerId: 1002, title: '《长安的荔枝》马伯庸亲签本', price: 58, coverImage: IMG.book, categoryId: 6, categoryName: '图书', condition: 1, tags: ['马伯庸', '签名'] }),

  // 服饰/包
  make({ id: 8, sellerId: 1006, title: 'COACH 女士手提单肩包 真皮 95 新', price: 899, originalPrice: 2380, coverImage: IMG.bag, categoryId: 3, categoryName: '鞋包', condition: 1, tags: ['COACH', '真皮', '中古'] }),
  make({ id: 9, sellerId: 1003, title: 'Nike Air Force 1 白色 42 码', price: 459, originalPrice: 799, coverImage: IMG.shoes, categoryId: 3, categoryName: '鞋包', condition: 2, tags: ['Nike', 'AF1', '潮鞋'] }),
  make({ id: 10, sellerId: 1003, title: '优衣库摇粒绒外套 M 码 卡其', price: 99, originalPrice: 299, coverImage: IMG.dress, categoryId: 2, categoryName: '服饰', condition: 2, tags: ['优衣库', '摇粒绒'] }),

  // 家居/咖啡
  make({ id: 11, sellerId: 1005, title: '德龙 EC685 半自动咖啡机 银色', price: 580, originalPrice: 1290, coverImage: IMG.coffee, categoryId: 5, categoryName: '家居', condition: 2, tags: ['德龙', '咖啡机'] }),
  make({ id: 12, sellerId: 1004, title: 'MSR Hubba Hubba 帐篷 双人三季', price: 1899, originalPrice: 3299, coverImage: IMG.tent, categoryId: 8, categoryName: '运动户外', condition: 2, tags: ['MSR', '帐篷', '户外'] }),

  // 游戏
  make({ id: 13, sellerId: 1007, title: 'Switch OLED 白色 港版 + 4 张游戏', price: 1880, originalPrice: 2599, coverImage: IMG.switch, categoryId: 7, categoryName: '潮玩', condition: 2, tags: ['Switch', '游戏机'] }),

  // 艺术
  make({ id: 14, sellerId: 1004, title: '原创丙烯装饰画 60x80 风景系列', price: 380, coverImage: IMG.painting, categoryId: 7, categoryName: '潮玩', condition: 1, tags: ['装饰画', '原创', '风景'] }),
  make({ id: 15, sellerId: 1007, title: 'Santa Cruz 滑板 8.0 全新', price: 480, originalPrice: 780, coverImage: IMG.skateboard, categoryId: 8, categoryName: '运动户外', condition: 1, tags: ['滑板', 'Santa Cruz'] }),

  // 美妆
  make({ id: 16, sellerId: 1003, title: 'Chanel 5 号香水 100ml 95 新', price: 680, originalPrice: 1280, coverImage: IMG.perfume, categoryId: 4, categoryName: '美妆', condition: 1, tags: ['Chanel', '香水'] }),

  // 杯子
  make({ id: 17, sellerId: 1005, title: '星巴克 樱花季限定马克杯', price: 89, coverImage: IMG.cup, categoryId: 5, categoryName: '家居', condition: 1, tags: ['星巴克', '杯子', '限定'] }),

  // 更多数码
  make({ id: 18, sellerId: 1001, title: 'iPad Pro 11 寸 M4 256G 深空黑', price: 7999, originalPrice: 9999, coverImage: IMG.iphone, categoryId: 1, categoryName: '数码', condition: 2, tags: ['iPad Pro', 'M4'] }),
  make({ id: 19, sellerId: 1001, title: 'Sony WH-1000XM5 头戴降噪耳机', price: 1599, originalPrice: 2899, coverImage: IMG.airpods, categoryId: 1, categoryName: '数码', condition: 2, tags: ['Sony', '降噪耳机'] }),
  make({ id: 20, sellerId: 1003, title: 'Stanley 保温杯 1.2L 粉色', price: 159, coverImage: IMG.cup, categoryId: 5, categoryName: '家居', condition: 1, tags: ['Stanley', '保温杯'] }),
]

// 类目
export const categories = [
  { id: 1, parentId: 0, name: '数码', level: 1 as const, icon: 'phone', sort: 1 },
  { id: 2, parentId: 0, name: '服饰', level: 1 as const, icon: 'shirt', sort: 2 },
  { id: 3, parentId: 0, name: '鞋包', level: 1 as const, icon: 'bag', sort: 3 },
  { id: 4, parentId: 0, name: '美妆', level: 1 as const, icon: 'lipstick', sort: 4 },
  { id: 5, parentId: 0, name: '家居', level: 1 as const, icon: 'home', sort: 5 },
  { id: 6, parentId: 0, name: '图书', level: 1 as const, icon: 'book', sort: 6 },
  { id: 7, parentId: 0, name: '潮玩', level: 1 as const, icon: 'star', sort: 7 },
  { id: 8, parentId: 0, name: '运动户外', level: 1 as const, icon: 'ball', sort: 8 },
]

export function getProductById(id: number) {
  return products.find(p => p.id === id)
}
