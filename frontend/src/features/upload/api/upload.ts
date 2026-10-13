import { apiFetch } from "@/lib/api/client"

export function uploadImage(file: File) {
  const form = new FormData()
  form.append("file", file)
  return apiFetch<{ url: string }>("/uploads/image", {
    method: "POST",
    body: form,
  })
}
