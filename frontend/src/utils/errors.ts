/** Extracts the backend's error message from an Axios error, falling back to a generic description. */
export const getErrorMessage = (error: unknown, fallback = 'The server did not respond. Check the connection and retry.'): string => {
  const err = error as { response?: { data?: { error?: { message?: string } } }; message?: string } | undefined;
  return err?.response?.data?.error?.message || err?.message || fallback;
};
