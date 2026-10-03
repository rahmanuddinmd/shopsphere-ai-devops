import { useState } from 'react';
import { SearchIcon } from './Icons.jsx';

export default function SearchBar({ onSearch, initial = '' }) {
  const [q, setQ] = useState(initial);
  return (
    <form
      role="search"
      className="searchbar"
      onSubmit={(e) => {
        e.preventDefault();
        if (q.trim()) onSearch(q.trim());
      }}
    >
      <SearchIcon />
      <label htmlFor="site-search" className="sr-only">Search products</label>
      <input id="site-search" type="search" value={q} onChange={(e) => setQ(e.target.value)}
        placeholder="Search phones, laptops, monitors…" />
    </form>
  );
}
