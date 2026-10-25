/**
 * Downscales and JPEG-compresses an image file to a base64 data URI before upload, so a
 * resident's phone photo (often several MB) doesn't get sent/stored at full size. Runs
 * entirely in the browser via canvas — no server round-trip.
 */
export function fileToCompressedDataUrl(file: File, maxDimension = 480, quality = 0.75): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('Could not read the selected file'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('Could not read the selected image'))
      img.onload = () => {
        const scale = Math.min(1, maxDimension / Math.max(img.width, img.height))
        const width = Math.round(img.width * scale)
        const height = Math.round(img.height * scale)

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas is not supported in this browser'))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.src = reader.result as string
    }
    reader.readAsDataURL(file)
  })
}

/**
 * Reads a non-image file (e.g. a PDF of meeting minutes) straight to a base64 data URI, with no
 * compression step — unlike fileToCompressedDataUrl, there's no canvas re-encode available for a
 * PDF. maxBytes guards against an oversized upload before it's even read (the backend enforces
 * its own cap independently on the resulting base64 string — see AnnouncementService).
 */
export function fileToDataUrl(file: File, maxBytes = 4_500_000): Promise<string> {
  return new Promise((resolve, reject) => {
    if (file.size > maxBytes) {
      reject(new Error(`File is too large (max ${Math.round(maxBytes / 1_000_000)}MB)`))
      return
    }
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('Could not read the selected file'))
    reader.onload = () => resolve(reader.result as string)
    reader.readAsDataURL(file)
  })
}
