import { defineStore } from 'pinia'

const listInFlightMap = new Map()
const detailInFlightMap = new Map()

const now = () => Date.now()

function cloneValue(value) {
  if (value == null) return value
  try {
    return JSON.parse(JSON.stringify(value))
  } catch (_) {
    return value
  }
}

function readBucket(state, namespace) {
  if (!state.namespaces[namespace]) {
    state.namespaces[namespace] = {
      lists: {},
      details: {},
    }
  }
  return state.namespaces[namespace]
}

function inflightKey(namespace, key) {
  return `${namespace}::${key}`
}

export const useResourceCacheStore = defineStore('resource-cache', {
  state: () => ({
    namespaces: {},
  }),
  actions: {
    readList(namespace, key, ttlMs = 0) {
      const entry = readBucket(this.$state, namespace).lists[key]
      if (!entry) return null
      if (ttlMs > 0 && now() - entry.cachedAt > ttlMs) return null
      return cloneValue(entry.data)
    },
    writeList(namespace, key, data) {
      readBucket(this.$state, namespace).lists[key] = {
        data: cloneValue(data),
        cachedAt: now(),
      }
    },
    invalidateLists(namespace) {
      readBucket(this.$state, namespace).lists = {}
    },
    readDetail(namespace, id, ttlMs = 0) {
      const entry = readBucket(this.$state, namespace).details[id]
      if (!entry) return null
      if (ttlMs > 0 && now() - entry.cachedAt > ttlMs) return null
      return cloneValue(entry.data)
    },
    writeDetail(namespace, id, data) {
      if (!id) return
      readBucket(this.$state, namespace).details[id] = {
        data: cloneValue(data),
        cachedAt: now(),
      }
    },
    invalidateDetail(namespace, id) {
      if (!id) return
      delete readBucket(this.$state, namespace).details[id]
    },
    async loadList(namespace, key, loader, { ttlMs = 0, force = false } = {}) {
      if (!force) {
        const cached = this.readList(namespace, key, ttlMs)
        if (cached) {
          return cached
        }
      }
      const taskKey = inflightKey(namespace, key)
      if (listInFlightMap.has(taskKey)) {
        return listInFlightMap.get(taskKey)
      }
      const task = Promise.resolve()
        .then(() => loader())
        .then((data) => {
          this.writeList(namespace, key, data)
          return cloneValue(data)
        })
        .finally(() => {
          listInFlightMap.delete(taskKey)
        })
      listInFlightMap.set(taskKey, task)
      return task
    },
    async loadDetail(namespace, id, loader, { ttlMs = 0, force = false } = {}) {
      if (!force) {
        const cached = this.readDetail(namespace, id, ttlMs)
        if (cached) {
          return cached
        }
      }
      const taskKey = inflightKey(namespace, id)
      if (detailInFlightMap.has(taskKey)) {
        return detailInFlightMap.get(taskKey)
      }
      const task = Promise.resolve()
        .then(() => loader())
        .then((data) => {
          this.writeDetail(namespace, id, data)
          return cloneValue(data)
        })
        .finally(() => {
          detailInFlightMap.delete(taskKey)
        })
      detailInFlightMap.set(taskKey, task)
      return task
    },
  },
})
