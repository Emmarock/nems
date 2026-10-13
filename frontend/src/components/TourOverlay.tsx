import { useEffect, useState, type CSSProperties } from 'react'
import { useTour } from '../tour/TourContext'

const SPOTLIGHT_PADDING = 8
const TOOLTIP_WIDTH = 320
const TOOLTIP_MARGIN = 16
/** Give the target page time to mount and fetch its own data (e.g. DashboardPage's stat cards
 * load async) before giving up and falling back to a centered, spotlight-less tooltip. */
const FIND_TARGET_TIMEOUT_MS = 5000
const FIND_TARGET_POLL_MS = 50

export function TourOverlay() {
  const { active, currentStep, stepIndex, steps, next, back, skip } = useTour()
  const [rect, setRect] = useState<DOMRect | null>(null)

  useEffect(() => {
    if (!active || !currentStep?.target) {
      setRect(null)
      return
    }
    const target = currentStep.target
    let timeoutId: number | undefined
    let elapsed = 0
    let cancelled = false

    function measure(): HTMLElement | null {
      return document.querySelector<HTMLElement>(`[data-tour="${target}"]`)
    }

    function poll() {
      if (cancelled) return
      const el = measure()
      if (el) {
        setRect(el.getBoundingClientRect())
        el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
        return
      }
      elapsed += FIND_TARGET_POLL_MS
      if (elapsed >= FIND_TARGET_TIMEOUT_MS) {
        setRect(null) // Give up - render the tooltip centered rather than leave the tour stuck.
        return
      }
      timeoutId = window.setTimeout(poll, FIND_TARGET_POLL_MS)
    }
    poll()

    function onReflow() {
      const el = measure()
      if (el) setRect(el.getBoundingClientRect())
    }
    window.addEventListener('resize', onReflow)
    window.addEventListener('scroll', onReflow, true)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
      window.removeEventListener('resize', onReflow)
      window.removeEventListener('scroll', onReflow, true)
    }
  }, [active, currentStep])

  if (!active || !currentStep) {
    return null
  }

  const isFirst = stepIndex === 0
  const isLast = stepIndex === steps.length - 1

  // The spotlight div's box-shadow is what actually darkens the screen (an infinite-spread
  // shadow paints everything outside the element's own box, leaving its interior as the natural
  // "cutout"). With no target rect, collapsing it to a zero-size point still darkens the full
  // viewport uniformly - no cutout, which is exactly what a centered welcome/closing step wants.
  const spotlightStyle = rect
    ? {
        top: rect.top - SPOTLIGHT_PADDING,
        left: rect.left - SPOTLIGHT_PADDING,
        width: rect.width + SPOTLIGHT_PADDING * 2,
        height: rect.height + SPOTLIGHT_PADDING * 2,
      }
    : { top: window.innerHeight / 2, left: window.innerWidth / 2, width: 0, height: 0 }

  return (
    <div className="tour-root" role="dialog" aria-modal="true" aria-label="Guided tour">
      <div className="tour-backdrop" onClick={(e) => e.stopPropagation()} />
      <div className="tour-spotlight" style={spotlightStyle} />
      <div className="tour-tooltip" style={rect ? tooltipPosition(rect) : undefined}>
        <div className="tour-tooltip-progress">
          {stepIndex + 1} / {steps.length}
        </div>
        <h3 className="tour-tooltip-title">{currentStep.title}</h3>
        <p className="tour-tooltip-body">{currentStep.body}</p>
        <div className="tour-tooltip-actions">
          <button type="button" className="btn btn-sm" onClick={skip}>
            Skip tour
          </button>
          <div style={{ display: 'flex', gap: 8 }}>
            {!isFirst && (
              <button type="button" className="btn btn-sm" onClick={back}>
                Back
              </button>
            )}
            <button type="button" className="btn btn-sm btn-primary" onClick={next}>
              {isLast ? 'Finish' : 'Next'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

/** Below the target when there's room, otherwise above it; clamped so it never runs off-screen. */
function tooltipPosition(rect: DOMRect): CSSProperties {
  const spaceBelow = window.innerHeight - rect.bottom
  const placeBelow = spaceBelow > 200

  let left = rect.left
  if (left + TOOLTIP_WIDTH > window.innerWidth - TOOLTIP_MARGIN) {
    left = window.innerWidth - TOOLTIP_WIDTH - TOOLTIP_MARGIN
  }
  left = Math.max(TOOLTIP_MARGIN, left)

  return placeBelow
    ? { top: rect.bottom + TOOLTIP_MARGIN, left, transform: 'none' }
    : { top: Math.max(TOOLTIP_MARGIN, rect.top - TOOLTIP_MARGIN), left, transform: 'translateY(-100%)' }
}
