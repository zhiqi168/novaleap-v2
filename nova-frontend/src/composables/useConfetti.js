import confetti from 'canvas-confetti'

const CONFETTI_CANVAS_ID = 'nova-global-confetti-canvas'
const CONFETTI_Z_INDEX = 9999

let confettiCanvas = null
let confettiInstance = null

const ensureCanvas = () => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return null

  if (confettiCanvas instanceof HTMLCanvasElement && document.body.contains(confettiCanvas)) {
    return confettiCanvas
  }

  const existingCanvas = document.getElementById(CONFETTI_CANVAS_ID)
  if (existingCanvas instanceof HTMLCanvasElement) {
    confettiCanvas = existingCanvas
    return confettiCanvas
  }

  const canvas = document.createElement('canvas')
  canvas.id = CONFETTI_CANVAS_ID
  canvas.setAttribute('aria-hidden', 'true')
  canvas.style.position = 'fixed'
  canvas.style.inset = '0'
  canvas.style.width = '100%'
  canvas.style.height = '100%'
  canvas.style.pointerEvents = 'none'
  canvas.style.zIndex = String(CONFETTI_Z_INDEX)

  document.body.appendChild(canvas)
  confettiCanvas = canvas
  return confettiCanvas
}

const getConfettiInstance = () => {
  const canvas = ensureCanvas()
  if (!canvas) return null

  if (!confettiInstance) {
    confettiInstance = confetti.create(canvas, {
      resize: true,
      useWorker: false,
    })
  }

  return confettiInstance
}

export const fireConfetti = (options = {}) => {
  const instance = getConfettiInstance()
  if (!instance) return Promise.resolve()

  try {
    return instance({
      zIndex: CONFETTI_Z_INDEX,
      ...options,
    })
  } catch (error) {
    console.error('Failed to fire confetti.', error)
    return Promise.resolve()
  }
}

export const resetConfetti = () => {
  if (!confettiInstance) return

  try {
    confettiInstance.reset()
  } catch (error) {
    console.error('Failed to reset confetti.', error)
  }
}
