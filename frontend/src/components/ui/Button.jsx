import { forwardRef } from 'react';
import styles from './Button.module.css';

const Button = forwardRef(function Button({
  children, variant = 'primary', type = 'button', loading = false,
  loadingText = 'Зареждане…', disabled = false, fullWidth = false, className = '', ...props
}, ref) {
  const appearance = ['primary', 'secondary', 'danger', 'ghost'].includes(variant) ? variant : 'primary';
  return (
    <button {...props} ref={ref} type={type} disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={[styles.button, styles[appearance], fullWidth && styles.fullWidth, className].filter(Boolean).join(' ')}>
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {loading ? loadingText : children}
    </button>
  );
});
export default Button;
