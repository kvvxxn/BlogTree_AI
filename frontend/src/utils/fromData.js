export function toFormData(record = {}) {
	const formData = new FormData();
	Object.entries(record).forEach(([key, value]) => {
		if (value !== undefined && value !== null) {
			formData.append(key, value);
		}
	});
	return formData;
}
