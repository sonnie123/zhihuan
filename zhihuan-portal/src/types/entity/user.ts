// 用户实体 (对齐 zhihuan-user 服务 UserDTO)
export type Gender = 0 | 1 | 2  // 0未知 1男 2女
export type UserType = 1 | 2  // 1个人 2商家
export type UserStatus = 1 | 2 | 3  // 1正常 2封禁 3注销
export type RealNameStatus = 0 | 1  // 0未实名 1已实名

export interface UserDTO {
  id: number
  username: string
  nickname: string
  avatar: string
  phone?: string
  email?: string
  gender?: Gender
  userType?: UserType
  status?: UserStatus
  realNameStatus?: RealNameStatus
  creditScore: number  // 0-100
  followerCount?: number
  followingCount?: number
  favoriteCount?: number
  registerTime: string
  lastLoginTime?: string
  bio?: string
}

export interface UserStats {
  sellCount: number
  buyCount: number
  favoriteCount: number
  footprintCount: number
}
