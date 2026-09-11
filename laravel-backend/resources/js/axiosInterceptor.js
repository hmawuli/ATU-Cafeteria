import axios from 'axios';

/**
 * Global Axios Response Interceptor
 * Automatically captures backend HTTP errors and presents non-intrusive toast notifications.
 */
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        let errorMessage = 'An unexpected network error occurred.';

        if (error.response) {
            const data = error.response.data;
            if (data && data.message) {
                errorMessage = data.message;
            } else if (error.response.status === 429) {
                errorMessage = 'Rate limit exceeded. Please wait a moment before retrying.';
            } else if (error.response.status === 401) {
                errorMessage = 'Session expired or unauthenticated. Please log in again.';
            } else if (error.response.status === 403) {
                errorMessage = 'You do not have permission to perform this action.';
            } else if (error.response.status === 404) {
                errorMessage = 'Requested API resource was not found.';
            } else if (error.response.status >= 500) {
                errorMessage = 'Server error encountered. Please try again shortly.';
            }
        } else if (error.request) {
            errorMessage = 'Unable to reach backend server. Please check your network connection.';
        }

        displayNonIntrusiveToast(errorMessage);
        return Promise.reject(error);
    }
);

function displayNonIntrusiveToast(message) {
    if (typeof document === 'undefined') return;

    let toastContainer = document.getElementById('api-toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'api-toast-container';
        toastContainer.className = 'fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm pointer-events-none';
        document.body.appendChild(toastContainer);
    }

    const toast = document.createElement('div');
    toast.className = 'bg-slate-900 text-white border-l-4 border-red-500 px-4 py-3 rounded-lg shadow-xl text-xs font-medium transition-all duration-300 opacity-0 transform translate-y-2 pointer-events-auto';
    toast.innerText = message;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.classList.remove('opacity-0', 'translate-y-2');
        toast.classList.add('opacity-100', 'translate-y-0');
    }, 10);

    setTimeout(() => {
        toast.classList.remove('opacity-100', 'translate-y-0');
        toast.classList.add('opacity-0', 'translate-y-2');
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

export default axios;
