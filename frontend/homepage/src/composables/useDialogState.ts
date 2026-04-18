import { ref } from 'vue'

const isWelcomeDialogOpen = ref(false)
const isEducationDialogOpen = ref(false)
const isInfoDialogOpen = ref(false)

export function useDialogState() {
	function setWelcomeDialogOpen(value: boolean) {
		isWelcomeDialogOpen.value = value
	}
	function setEducationDialogOpen(value: boolean) {
		isEducationDialogOpen.value = value
	}
	function setInfoDialogOpen(value: boolean) {
		isInfoDialogOpen.value = value
	}

	return {
		isWelcomeDialogOpen,
		isEducationDialogOpen,
		isInfoDialogOpen,
		setWelcomeDialogOpen,
		setEducationDialogOpen,
		setInfoDialogOpen,
	}
}
