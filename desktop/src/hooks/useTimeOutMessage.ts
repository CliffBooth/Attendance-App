import { useEffect, useState } from 'react';

export default function useTimeOutMessage(interval = 3000) {
    const [message, setMessage] = useState<string>('');

    useEffect(() => {
        let timeout = setTimeout(() => setMessage(''), interval);
        return () => {
            clearTimeout(timeout);
        };
    }, [message]);

    return {message, setMessage};
}
