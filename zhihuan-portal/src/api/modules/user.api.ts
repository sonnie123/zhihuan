import http from '../client'
import type { UserDTO, UserStats } from '@/types/entity/user'

export const userApi = {
  getCurrent: () => http.get<UserDTO>('/user/current'),
  getById: (id: number) => http.get<UserDTO>('/user/' + id),
  getStats: (id: number) => http.get<UserStats>('/user/' + id + '/stats'),
  getFollowers: () => http.get<UserDTO[]>('/user/followers'),
  getFollowings: () => http.get<UserDTO[]>('/user/followings'),
}