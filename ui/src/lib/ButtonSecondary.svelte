<script>
    import {Button} from "flowbite-svelte";

</script>

<Button class="hover:bg-secondary-700 hover:text-white border border-secondary-700 bg-white text-secondary-700"
        on:click>
    <slot />
</Button>
