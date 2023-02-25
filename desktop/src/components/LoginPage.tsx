import { Formik } from 'formik';
import useAuth from '../services/ApiService';

const LoginPage = () => {

    const handleSubmit = async ({email}: {email: string}, setSubmitting: (s: boolean) => void) => {
        console.log(email)
        setSubmitting(true)
        const result = await signIn({email})
        if (result.status === 'success') {
            //automatically save user in the localstorage and take to home page
            console.log('success!')
            console.log(result)
        } else {
            //display error
            console.log('failure!')
            console.log(result)
        }
    }

    const { signIn } = useAuth()

    return (
        <div className="flex">
            <div className="mx-auto bg-gray-200 shadow-lg p-5 space-y-7 text-lg">
                <h1 className="text-center font-bold">Authentication</h1>
                <Formik
                    initialValues={{
                        email: '',
                    }}
                    onSubmit={(values, { setSubmitting }) => {
                        handleSubmit(values, setSubmitting)
                    }}
                >
                    {formik => (
                        <form
                            className="flex flex-col text-center p-3 space-y-5"
                            onSubmit={formik.handleSubmit}
                        >
                            <label htmlFor="email">Email</label>
                            <input
                                id="email"
                                className="input-login"
                                type="text"
                                {...formik.getFieldProps('email')}
                            />
                            {formik.touched.email && formik.errors.email ? (
                                <div>{formik.errors.email}</div>
                            ) : null}

                            <button
                                className="bg-gray-700 text-xl text-white p-2 rounded-md hover:bg-gray-600"
                                type="submit"
                            >
                                Submit
                            </button>
                        </form>
                    )}
                </Formik>
            </div>
        </div>
    );
};

export default LoginPage;
