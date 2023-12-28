<script>
    import "../app.pcss";
    import { page } from '$app/stores';
    import {Navbar, NavLi, NavUl, NavHamburger, NavBrand} from 'flowbite-svelte';
    $: activeUrl = $page.url.pathname;
    let activeClass = 'text-white bg-green-700 md:bg-transparent md:text-green-700 md:dark:text-white dark:bg-green-600 md:dark:bg-transparent';
    let nonActiveClass = 'text-gray-700 hover:bg-gray-100 md:hover:bg-transparent md:border-0 md:hover:text-green-700 dark:text-gray-400 md:dark:hover:text-white dark:hover:bg-gray-700 dark:hover:text-white md:dark:hover:bg-transparent';
</script>

<Navbar rounded color="primary">
    <NavBrand href="">
        <img src="/src/lib/images/Documind%20Logo.png" class="me-3 h-6 sm:h-9" alt="Documind Logo" />
        <span class="self-center whitespace-nowrap text-xl font-semibold dark:text-white">Documind</span>
    </NavBrand>
    <NavHamburger></NavHamburger>
    <NavUl {activeUrl} {activeClass} {nonActiveClass}>
        <NavLi href="/upload">Upload</NavLi>
        <NavLi href="/view">View</NavLi>
        <NavLi href="/query">Query</NavLi>
        <NavLi href="/quiz">Quiz</NavLi>
        <NavLi href="/contact">Contact</NavLi>
    </NavUl>
</Navbar>
<slot></slot>