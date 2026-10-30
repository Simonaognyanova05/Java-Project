import { forwardRef, useId } from 'react';
import styles from './FormField.module.css';

const FormField = forwardRef(function FormField({
  as: Control = 'input', id, label, hint, error, required = false,
  children, leading, trailing, className = '', controlClassName = '',
  'aria-describedby': describedBy, ...props
}, ref) {
  const generatedId = useId();
  const controlId = id || generatedId;
  const hintId = hint ? controlId + '-hint' : undefined;
  const errorId = error ? controlId + '-error' : undefined;
  const description = [describedBy, hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div className={[styles.field, className].filter(Boolean).join(' ')}>
      <label htmlFor={controlId}>{label}{required && <span aria-hidden="true"> *</span>}</label>
      <div className={styles.controlWrapper}>
        {leading && <span className={styles.leading} aria-hidden="true">{leading}</span>}
        <Control {...props} ref={ref} id={controlId} required={required}
          aria-describedby={description} aria-invalid={error ? true : props['aria-invalid']}
          className={[styles.control, leading && styles.withLeading, trailing && styles.withTrailing, controlClassName].filter(Boolean).join(' ')}>
          {children}
        </Control>
        {trailing && <span className={styles.trailing} aria-hidden="true">{trailing}</span>}
      </div>
      {hint && <small id={hintId}>{hint}</small>}
      {error && <small id={errorId} className={styles.error} role="alert">{error}</small>}
    </div>
  );
});
export default FormField;
