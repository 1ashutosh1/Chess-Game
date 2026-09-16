import { Link } from 'react-router-dom'

// Renders a <Link> styled as a button when `to` is given (navigation),
// otherwise a real <button> (actions like New Game / Create Game / Join).
function Button({ to, variant = 'primary', type = 'button', className = '', children, ...props }) {
  const classes = `btn btn-${variant}${className ? ` ${className}` : ''}`

  if (to) {
    return (
      <Link to={to} className={classes}>
        {children}
      </Link>
    )
  }

  return (
    <button type={type} className={classes} {...props}>
      {children}
    </button>
  )
}

export default Button
