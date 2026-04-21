const cloudName = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME as string | undefined

function joinTransforms(transforms: string[]): string {
  return transforms.filter(Boolean).join(',')
}

export function buildCloudinaryImageUrl(publicId: string, transforms: string[] = ['f_auto', 'q_auto']): string {
  if (!cloudName || !publicId) return ''
  return `https://res.cloudinary.com/${cloudName}/image/upload/${joinTransforms(transforms)}/${publicId}`
}

export function buildCloudinarySrcSet(publicId: string, widths: number[], extraTransforms: string[] = []): string {
  if (!cloudName || !publicId || widths.length === 0) return ''
  return widths
    .map((width) => {
      const url = buildCloudinaryImageUrl(publicId, ['f_auto', 'q_auto', ...extraTransforms, `w_${width}`])
      return `${url} ${width}w`
    })
    .join(', ')
}
