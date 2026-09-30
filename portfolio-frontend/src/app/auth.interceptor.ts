import { HttpInterceptorFn } from '@angular/common/http';

const TOKEN_KEY = 'portfoliopro.jwt';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (!token || !request.url.startsWith('/api/')) return next(request);

  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

export { TOKEN_KEY };
