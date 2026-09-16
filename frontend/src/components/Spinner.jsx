// A centered circular loading indicator for full-page loading states
// (e.g. restoring a game on refresh), as opposed to inline button spinners.
function Spinner() {
  return (
    <div className="spinner-overlay" role="status" aria-label="Loading">
      <div className="spinner" />
    </div>
  )
}

export default Spinner
